#!/usr/bin/env ruby

require 'json'
require 'fileutils'
require 'pry'
require 'net/sftp'
require 'net/ssh'

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
  config_location = String.new
  if ARGV.length > 0
    config_location = ARGV[0]
  end

  config_location = "/etc/tealium/pc1-migration.json" unless config_location

  puts "Configuration file location is at : #{config_location}"
  return config_location
end

def uploadFileSystemData(config, account_name)
  local_account_dir = checkDataExists(config.fetch("local_tmp_dir"), account_name)
  scp_user, scp_host, utui_tmp_dir = getSCPValues(config)

  utui_accounts_dir = config.fetch("accounts_dir")
  utui_tmp_account_dir = File.join(utui_tmp_dir, account_name)

  puts "Starting to SCP data from directory #{local_account_dir} to #{utui_tmp_dir}\n"
  begin
    ssh = Net::SSH.start(scp_host, scp_user )
    sftp = ssh.sftp
    ssh.exec!("sudo test -d #{utui_tmp_dir}")

    uploadingData(sftp, local_account_dir, utui_tmp_account_dir, ssh)

    puts "Copying data from #{utui_tmp_account_dir} to #{utui_accounts_dir}\n"
    ssh.exec!("sudo cp -r #{utui_tmp_account_dir} #{utui_accounts_dir}")

    final_account_dir = File.join(utui_accounts_dir, account_name)

    ssh.exec!("sudo chown -R 'www-data:www-data' #{final_account_dir}")
    ssh.exec!("sudo rm -r #{utui_tmp_account_dir}")
    ssh.close
  rescue => e
    puts "FRACASAR: could not SCP data to #{utui_accounts_dir} error received: #{e}\n"
    exit 1
  end
  puts "Finish Uploading data from #{local_account_dir} to #{utui_accounts_dir}\n"
end

def checkDataExists(local_tmp_dir, account_name)
  puts "Checking if there is data to Import \n"
  local_account_dir = File.join(local_tmp_dir, account_name)
  unless Dir.exists?(local_account_dir)
    puts "FRACASAR: Directory #{local_account_dir} does not exists cannot import data\n"
    exit 1
  end
  return local_account_dir
end

def uploadingData(sftp, local_tmp_dir, utui_account_tmp_dir, ssh)
  puts "Uploading data from #{local_tmp_dir} to #{utui_account_tmp_dir}\n"
  sftp.mkdir!(utui_account_tmp_dir)
 # ssh.exec!("sudo mkdir -r #{utui_account_tmp_dir}")

  sftp.upload!(local_tmp_dir, utui_account_tmp_dir) do |event, downloader, *args|
    case event
      when :put then
        puts "Writing #{args[2].length} bytes from #{args[0].local} starting at #{args[1]}\n"
      when :close then
        puts "Finished with #{args[0].remote} \n"
      when :finish then
        puts "All done uploading files/directories! \n"
    end
  end
end

def getSCPValues(config)
  scp_user = config.fetch("ssh_user")
  scp_host = config.fetch("import_ssh_host")
  utui_tmp_dir = config.fetch("utui_tmp_dir")
  return scp_user, scp_host, utui_tmp_dir
end

def insertMongoData(config, account_name, local_tmp_dir)
  mongo_user, mongo_pwd, mongo_host, mongo_db, mongo_coll = getMongoValues(config)

  puts "Getting ready to inserting mongo data for Account: #{account_name} to Database #{mongo_db}\n"
  mongo_coll.each do | coll |

    #binding.pry
    puts "Getting Mongo Export Data for Account: #{account_name}, database: #{mongo_db}, and collection #{coll}"
    code = system("mongoimport -h #{mongo_host} -d #{mongo_db} -c #{coll} --authenticationDatabase admin -u #{mongo_user} -p #{mongo_pwd} --file #{local_tmp_dir}/#{coll}.json --upsert")
    #code = system("mongoimport -h #{mongo_host} -d #{mongo_db} -c #{coll} --file #{local_tmp_dir}/#{coll}.json --upsert")

    unless code
       puts "FRACASAR: to import Mongo data for collection : #{coll} for Account : #{account_name} Error : #{code}"
       exit 1
    end
  end
end

def getMongoValues(config)

  mongo_coll = config.fetch("mongo_collections")
  mongo_host = config.fetch("import_mongo_host")
  mongo_db = config.fetch("mongo_db")
  mongo_user = config.fetch("mongo_user")
  mongo_pwd = config.fetch("mongo_pwd")

  return mongo_user, mongo_pwd, mongo_host, mongo_db, mongo_coll
end

if __FILE__ == $PROGRAM_NAME

  config_location = getCommandArguments
  config = getConfigFile(config_location)

  account_name = config.fetch("account_name")
  local_tmp_dir = config.fetch("local_tmp_dir")

  puts "Importing data for Account: #{account_name.upcase}\n"
  uploadFileSystemData(config, account_name)

  insertMongoData(config, account_name, local_tmp_dir)

  puts "Import Migration Script Finish \n"
  exit 0
end
