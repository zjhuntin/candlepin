require 'spec_helper'

describe "Owner Resource" do
  include_context "standard"

  it "creates an owner" do
    user_client = Candlepin::BasicAuthClient.new
    key = rand_string('owner')
    res = user_client.create_owner(
      :key => key,
      :display_name => key
    )
    expect(res).to be_success
    @owners << res.ok_content
  end

  it 'updates an owner' do
    new_name = rand_string('new_owner_name')
    o = owner_client.update_owner(
      :display_name => new_name
    ).ok_content
    expect(o[:displayName]).to eq(new_name)
  end

  it "lists only requested consumer types" do
    owner_client.register(
      :username => owner_user[:username],
      :name => rand_string
    )
    res = owner_client.get_owner_consumers(:types => :system)
    expect(res).to be_success
    expect(res.content.length).to eq(1)

    res = owner_client.get_owner_consumers(:types => :candlepin)
    expect(res).to be_success
    expect(res.content).to be_empty
  end

  it "does not list another owner's consumers" do
    user_client.fail_fast = true
    owner2 = new_owner
    owner2_user = new_owner_user(:owner => owner2[:key])

    user_client.register(
      :username => owner2_user[:username],
      :name => rand_string,
      :owner => owner2[:key]
    )

    owner_consumer = user_client.register(
      :username => owner_user[:username],
      :name => rand_string,
      :owner => owner[:key]
    ).ok_content

    consumers = owner_client.get_owner_consumers.ok_content
    expect(consumers.length).to eq(1)
    expect(consumers.first[:name]).to eq(owner_consumer[:name])
  end

  it "shows consumers their service levels" do
    consumer_client = owner_client.register_and_get_client(
      :name => rand_string('consumer'))

    prod = new_product(
      :attributes => { :support_level => 'VIP' }
    )

    owner_client.create_pool(:product_id => prod[:id]).ok_content
    levels = owner_client.get_owner_service_levels.ok_content
    expect(levels).to eq(['VIP'])

    owner2 = new_owner
    res = consumer_client.get_owner_service_levels(:owner => owner2[:key])
    expect(res).to be_missing
  end

  it "creates owners with parents" do
    child_owner = new_owner(
      :parent_owner => owner,
    )
    expect(child_owner[:parentOwner][:id]).to eq(owner[:id])
  end

  it "fails with a bad parent owner" do
    bad_owner = {
      :key => 'bad',
      :display_name => 'bad',
      :id => 'bad',
    }

    res = user_client.create_owner(
      :parent_owner => bad_owner,
    )
    expect(res).to be_bad_request
  end

  it "lists pools for owners" do
    pools = owner_client.get_owner_pools.ok_content
    expect(pools).to be_empty
    owner_client.create_pool(:product_id => product[:id]).ok_content
    pools = owner_client.get_owner_pools.ok_content
    expect(pools).to_not be_empty
  end

  xit "lists pools for owners in pages" do
  end

  xit "lists pools for owners in pages per consumer" do
  end

  it "auto-creates owners via refresh pools" do
    new_key = rand_string('new_owner')
    user_client.refresh_pools(
      :owner => new_key,
      :auto_create_owner => true
    )

    new_org = user_client.get_owner(:owner => new_key).ok_content
    expect(new_org[:key]).to eq(new_key)

    pools = user_client.get_owner_pools(:owner => new_key).ok_content
    expect(pools).to be_empty
  end

  it "restricts refresh pools to superadmins" do
    [false, true].each do |super_admin|
      pass = rand_string('ro_pass')
      user = new_owner_user(
        :owner => owner[:key],
        :password => pass,
        :super_admin => super_admin)

      client = Candlepin::BasicAuthClient.new(
        :username => user[:username],
        :password => pass
      )

      res = client.refresh_pools(:owner => owner[:key])
      if super_admin
        expect(res).to be_success
      else
        expect(res).to be_forbidden
      end
    end
  end
end
