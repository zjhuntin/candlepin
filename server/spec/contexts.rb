require_relative '../client/ruby/candlepin'

shared_context "standard" do
  let!(:user_client) { Candlepin::BasicAuthClient.new }
  let!(:no_auth_client) { Candlepin::NoAuthClient.new }

  let(:owner) do
    new_owner
  end

  let(:owner_client) do
    client = Candlepin::BasicAuthClient.new
    client.key = owner[:key]
    client
  end

  let(:owner_user) do
    new_owner_user(:owner => owner[:key], :super_admin => true)
  end

  let(:role) do
    new_role
  end

  let(:content) do
    new_content
  end

  let(:product) do
    new_product
  end
end
