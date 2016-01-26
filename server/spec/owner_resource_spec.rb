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
  end

  it 'updates an owner' do
    old_name = owner[:displayName]
    res = owner_client.update_owner(
      :display_name => rand_string
    )
    expect(res).to be_success
    expect(res.content[:displayName]).to_not eq(old_name)
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
    owner2_user = new_owner_user(:owner => owner2)

    user_client.register(
      :username => owner2_user[:username],
      :name => rand_string,
      :owner => owner2[:key]
    )

    owner_consumer = user_client.register(
      :username => owner_user[:username],
      :name => rand_string,
      :owner => owner[:key]
    ).content

    consumers = owner_client.get_owner_consumers.content
    expect(consumers.length).to eq(1)
    expect(consumers.first[:name]).to eq(owner_consumer[:name])
  end

  it "shows consumers their service levels" do
    consumer_client = owner_client.register_and_get_client(
      :name => rand_string('consumer'))

    prod_id = rand_string('product')
    owner_client.fail_fast = true
    owner_client.create_product(
      :product_id => prod_id,
      :name => prod_id,
      :attributes => { :support_level => 'VIP' }
    ).content

    owner_client.create_pool(:product_id => prod_id).content
    levels = owner_client.get_owner_service_levels.content
    expect(levels).to eq(['VIP'])

    owner2 = new_owner
    res = consumer_client.get_owner_service_levels(:owner => owner2[:key])
    expect(res).to be_missing
  end

  it "creates owners with parents" do
    child_owner = user_client.create_owner(
      :parent_owner => owner[:key],
      :key => rand_string('child_owner'),
      :display_name => rand_string('child_owner')
    ).ok_content
    expect(child_owner[:parent_owner][:id]).to eq(owner[:id])
  end
end
