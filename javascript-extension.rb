#!/usr/bin/env ruby

require 'json'
require 'fileutils'
require 'pry'
require 'mongo'

JAVASCRIPT_EXTENSION_PERM_CONST = "EXTENSIONS_JAVASCRIPT"
JAVASCRIPT_PROFILE_PERM_CONST = "JS_DRAFT_PROMOTION"
EMAIL_CONST = "email"
PUBLISH_PROD_CONST = "PUBLISH_PROD"

def getConfigFile(config)
  puts "reading config file to get proper configuration"

  file = nil
  config_values = nil
  begin
    file = File.read(config)
    config_values = JSON.parse(file)
  rescue => e
    puts "FRACASAR: There was an error trying to read the config file : #{e}"
    exit 1
  end

  puts "Successfully read the config file"
  return config_values
end

def getCommandArguments
  puts "getting command line argument for location of config"
  config_location = nil
  if ARGV.length > 0
    config_location = ARGV[0]
  end

  config_location = "/etc/tealium/mongo_config_stats.json" unless config_location

  puts "Configuration file location is at : #{config_location}"
  return config_location
end

def getMongoCoreDB(mongo_host, mongo_db, permission)
  puts "Attemptig to connect to MongoDB\n"
  client = Mongo::Client.new([mongo_host], :database => mongo_db)

  puts "Getting the permission cache collection \n"
  permission_coll = client[permission]

  raise Exception, "Failed to get the user collection and permission cache collection" unless permission_coll
  return client, permission_coll
end

def getJSUsersStats(permission_coll, mongo_client)
  getAllPermissionCacheRecordsCount(permission_coll)

  documents = getAllPermissionCacheRecordsWithJSPermission(permission_coll)
  mongo_client.close

  js_users = Hash.new{|key, value| key[value] = []}
  no_prod_perm = Hash.new{|hsh,key| hsh[key] = {} }
  puts "Iterating through all Users that have #{JAVASCRIPT_EXTENSION_PERM_CONST} permission\n\n"
  documents.each do | doc |
    email = doc.fetch(EMAIL_CONST)
    account = doc.fetch("account")
    profiles = doc.fetch("profiles")

    js_users = getAllUsersWithJSPermission(js_users, email, account)
    no_prod = getAllUsersWithJSPermissionNoProd(profiles, email, account)

    unless no_prod.nil?
      no_prod_perm = no_prod.merge(no_prod_perm) {|key, val1, val2| val1.to_a + val2.to_a}
    end
  end
  getUsersJSStats(no_prod_perm, js_users)
end

def getAllPermissionCacheRecordsCount(permission_coll)
  puts "Querying all permission cache records\n\n"
  all_records = permission_coll.count()
  puts "HERE are all the Permission Cache Documents: #{all_records}\n\n"
end

def getAllPermissionCacheRecordsWithJSPermission(permission_coll)
  puts "Querying all permission cache records that have the #{JAVASCRIPT_EXTENSION_PERM_CONST}\n"
  documents = permission_coll.find(:permissions => {"$in" => [JAVASCRIPT_EXTENSION_PERM_CONST]}).sort([EMAIL_CONST, 1])
  puts "HERE are all the Permission Cache Documents with #{JAVASCRIPT_EXTENSION_PERM_CONST} #{documents.count}\n\n"
  return documents
end

def getAllUsersWithJSPermission(js_users, email, account)
  puts "User: #{email} has the #{JAVASCRIPT_EXTENSION_PERM_CONST} for account: #{account}\n\n"
  unless js_users.key?(email)
    accounts = [account]
    js_users.store(email, accounts)
  else
    js_users[email].push(account)
  end
  return js_users
end

def getAllUsersWithJSPermissionNoProd(profile_list, email, account)
    no_prod_perm = Hash.new{|hsh,key| hsh[key] = {} }
    profile_list.each do |list|
      profile = list.pop
      profile_perms = profile.fetch("permissions")

      if profile_perms.include?(PUBLISH_PROD_CONST)
        #this users has the PUBLISH_PROD no need to store
        return no_prod_perm
      end

      puts "User: #{email} has the #{JAVASCRIPT_EXTENSION_PERM_CONST} but does not have the
       #{PUBLISH_PROD_CONST} permission \n\n"

      profile_name = profile.fetch("profile")
      no_prod_perm[email].store(account, profile_name)
      return no_prod_perm
    end
end

def getUsersJSStats(no_prod, js_users)
  puts "Total of number of users that have the #{JAVASCRIPT_EXTENSION_PERM_CONST} permission
  across all accounts #{js_users.size}\n\n"
  puts "This is the HASH that has all users across all accounts that have the #{JAVASCRIPT_EXTENSION_PERM_CONST} :
  #{js_users}\n\n"
  puts "Totol of number of users that have the #{JAVASCRIPT_EXTENSION_PERM_CONST} permission but does not have the #{PUBLISH_PROD_CONST} permission: #{no_prod.size}\n\n"
  puts "THIS IS THE HASH FOR ALL USERS THAT DON'T HAVE THE #{PUBLISH_PROD_CONST} permission : #{no_prod}\n\n"

  puts "HERE IS HOW MANY USERS WILL BE AFFECTED BY ADDING THE NEW PERMISSION: #{no_prod.size}\n\n"

end

#this function will retrieves all the mongo configurations
def getMongoValues(config)
  puts "Getting Mongo configuration values\n"

  permission_coll = config.fetch("mongo_collection")
  mongo_host = config.fetch("mongo_host")
  mongo_db = config.fetch("mongo_db")

  raise ArgumentError, "Could not find Permission collection" if permission_coll.empty?
  return mongo_host, mongo_db, permission_coll
end

if __FILE__ == $PROGRAM_NAME

  config_location = getCommandArguments
  config = getConfigFile(config_location)

  mongo_host, mongo_db, permission = getMongoValues(config)

  puts "Connecting to Mongo #{mongo_db} DB from host #{mongo_host}\n"
  mongo_client, permission_coll = getMongoCoreDB(mongo_host, mongo_db, permission)

  puts "Getting User's stats for the EXTENSIONS_JAVASCRIPT permission\n"
  getJSUsersStats(permission_coll, mongo_client)

  puts "DONE WITH SCRIPT!!!!!!!\n"
  exit 0
end
