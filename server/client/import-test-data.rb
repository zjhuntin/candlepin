#! /usr/bin/env ruby

require 'logger'
require 'optparse'
require 'ostruct'
require 'json'
require 'concurrent'

# We need to get the swagger-client directory on the load path because the swagger_client.rb file uses
# require to pull in all the submodules instead of require_relative
$LOAD_PATH.unshift("#{File.dirname(__FILE__)}/swagger-client/lib")
require 'swagger_client'


module Candlepin
  class Importer
    attr_reader :log
    attr_reader :json
    attr_reader :options

    def initialize(options)
      @options = options
      @log = Logger.new(STDOUT)
      @log.datetime_format = '%Y-%m-%d %H:%M:%S'
      @log.formatter = proc do |severity, time, progname, msg|
        "[#{time}] - #{severity}: #{msg}\n"
      end

      @log.level = @options.verbose ? Logger::DEBUG : Logger::INFO
      @log.info("Beginning import")
      @log.debug(@options)
    end

    def load_data(data_file)
      log.info("Loading #{data_file}")
      @json = JSON.parse(File.read(data_file))
    end

    def import
      SwaggerClient.configure do |config|
        config.host = 'localhost:8443'
        config.username = 'admin'
        config.password = 'admin'
        config.debugging = true if options.verbose
        config.logger = log

        if File.exist?('/etc/candlepin/certs/candlepin-ca.crt')
          config.ssl_ca_cert = '/etc/candlepin/certs/candlepin-ca.crt'
          config.verify_ssl_host = false if config.host.start_with?('localhost')
        else
          config.verify_ssl = false
          config.verify_ssl_host = false
        end
      end
      log.debug(SwaggerClient::Configuration.default.password)

      import_users(json['users'])
      import_owners(json['owners'])

      log.close
    end

    private
    def on_http_error(reason, messages)
      if messages.key?(reason.code.to_s)
        log.warn(messages[reason.code.to_s])
      else
        log.error(reason)
      end
    end

    # Collect all promises under one promise and execute all of them.  Block until
    # all promises are complete and aggregate whether the promise was fulfilled or not
    def block_for_collective_execution(*promises)
      statuses = []
      composite = Concurrent::Promise.new do
        completed = promises.collect do |p|
          p.execute if p.unscheduled?
          p.wait
          p
        end
        statuses << completed.map do |p|
          p.fulfilled?
        end
        statuses
      end
      # Time out after 20 seconds
      composite.execute.value!(20)
    end

    def import_owners(owners)
      promises = []
      owners.each do |json_owner|
        log.info("Importing owner #{json_owner['name']}")
        owner = SwaggerClient::Owner.new(
          :key => json_owner['name'] + '5',
          :displayName => json_owner['displayName'],
        )
        if json_owner.key?('contentAccessModeList')
          owner.content_access_mode_list = json_owner['contentAccessModeList']
        end

        api = SwaggerClient::OwnersApi.new
        p = Concurrent::Promise.new do
          api.create_owner(owner)
        end
        p.on_error { |r| on_http_error(r, "409" => "Could not create owner #{owner.display_name}") }

        child = p.on_success do |r|
          log.info("Created #{owner.key}")
          default = SwaggerClient::ActivationKey.new(:name => "default_key")
          api.create_activation_key(owner.key, default)

          awesome_os = SwaggerClient::ActivationKey.new(:name => "awesome_os_pool")
          api.create_activation_key(owner.key, awesome_os)
        end
        child.on_error { |r| on_http_error(r, "409" => "Could not create activation keys for #{owner.display_name}") }

        # Execute the child promises.  Parents will be executed before children, so that ensures
        # that owners are created before we try to add activation keys to them
        promises << child.execute
      end

      block_for_collective_execution(*promises)
    end

    def import_users(users)
      promises = []
      users.each do |credentials|
        log.info("Importing #{credentials['username']}")
        api = SwaggerClient::UsersApi.new
        user = SwaggerClient::UserCreationRequest.new(
          :username => credentials['username'],
          :password => credentials['password'],
          :super_admin => credentials.key?('superadmin') ? credentials['superadmin'] : false
        )
        p = Concurrent::Promise.new do
          api.create_user(user)
        end
        p.on_error { |r| on_http_error(r, "409" => "User #{user.username} already exists")}

        promises << p.execute
      end
      block_for_collective_execution(*promises)
    end
  end
end

options = OpenStruct.new
options.verbose = false

parser = OptionParser.new do |opts|
  opts.banner = "Usage: import-test-data.rb [options] <test-data-file>"

  opts.on("-v", "--[no-]verbose", "Run verbosely") do |v|
    options.verbose = v
  end

  opts.on_tail("-h", "--help", "Show this message") do
    puts opts
    exit
  end
end

parser.parse!(ARGV)

importer = Candlepin::Importer.new(options)
if ARGV.size != 1
  puts parser
  exit
end
data_file = ARGV[0]

importer.load_data(data_file)
importer.import
