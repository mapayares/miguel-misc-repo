#!/usr/bin/env ruby

require 'json'
require 'fileutils'
require 'pry'
require 'net/ssh'
require 'net/sftp'

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
  puts "getting command line argument for location of config\n"
  config_location = String.new
  if ARGV.length > 0
    config_location = ARGV[0]
  end
  
  config_location = "/etc/tealium/pc1-migration.json" unless config_location
  
  puts "Configuration file location is at : #{config_location}"
  return config_location  
end

def createTmpDirectory(local_tmp_dir)
  
  puts "Creating temporary directory to store migration data at location: #{local_tmp_dir}\n"
  begin
    FileUtils.rm_rf(local_tmp_dir) if Dir.exists?(local_tmp_dir)
    FileUtils.mkdir(local_tmp_dir)
  rescue => e
    puts "FRACASAR: to create temporary directory received error: #{e}\n"
    exit 1
  end
  puts "Successfully created temporary directory location is: #{local_tmp_dir}\n"
end

def getMongoExportData(config, account_name, local_tmp_dir)
  
  mongo_coll = config.fetch("mongo_collections")
  mongo_host = config.fetch("export_mongo_host")
  mongo_db = config.fetch("mongo_db")
   
  puts "Getting ready to start collection mongo data for Account: #{account_name} and place it in location #{local_tmp_dir}\n"
  mongo_coll.each do | coll |
    
    mongo_query = "{account : '#{account_name}' }"
    
    if 'users'.eql?(coll)
      mongo_query = "{primary_account : '#{account_name}' }"
    end
        
    #binding.pry   
    puts "Getting Mongo Export Data for Account: #{account_name}, database: #{mongo_db}, and collection #{coll}\n"
    #return_value = system("mongoexport -h #{mongo_host} -d #{mongo_db} -c #{coll} -q '#{mongo_query}' -o #{local_tmp_dir}/#{coll}.json")
    return_value = `mongoexport -h #{mongo_host} -d #{mongo_db} -c #{coll} -q "#{mongo_query}" -o #{local_tmp_dir}/#{coll}.json`
    
    unless return_value
       puts "FRACASAR: to get Mongo data for collection : #{coll} for Account : #{account_name}\n"
       exit 1
    end
  end  
end

def getFileSystemData(config, account_name, local_tmp_dir)
  scp_user, scp_host, utui_account_dir = getSCPValues(config, account_name)
  utui_tmp_dir = config.fetch("utui_tmp_dir")
  local_account_tmp_dir = File.join(local_tmp_dir, account_name)
  
  puts "Starting to SCP data from directory #{utui_account_dir} to #{local_account_tmp_dir}\n"
  begin
    ssh = Net::SSH.start(scp_host, scp_user )
    ssh.exec("sudo test -d #{utui_account_dir}")
        
    puts "Copying data from #{utui_account_dir} to #{utui_tmp_dir}\n"
    ssh.exec!("sudo cp -r #{utui_account_dir} #{utui_tmp_dir}")
    sftp = ssh.sftp
            
    utui_account_tmp_dir = File.join(utui_tmp_dir, account_name)
    ssh.exec!("sudo chown -R #{scp_user}:#{scp_user} #{utui_account_tmp_dir}")
    downloadFileSystemData(sftp, local_account_tmp_dir, utui_account_tmp_dir, scp_user)
    ssh.exec!("sudo rm -r #{utui_account_tmp_dir}")
    ssh.close
  rescue => e
    puts "FRACASAR: could not SCP data from #{local_account_tmp_dir} error received: #{e}\n"
    exit 1
  end
  puts "Finish Downloading data from #{utui_account_tmp_dir} to #{local_account_tmp_dir}\n"
end

def downloadFileSystemData(sftp, local_account_tmp_dir, utui_tmp_dir, scp_user)
  puts "Downloading data from #{utui_tmp_dir} to #{local_account_tmp_dir}\n"
    
  sftp.download!(utui_tmp_dir, local_account_tmp_dir, :recursive => true) do |event, downloader, *args|
    case event
      when :get then
        puts "Writing #{args[2].length} bytes to #{args[0].local} starting at #{args[1]}\n"
      when :close then
        puts "Finished with #{args[0].remote}\n"
      when :finish then
        puts "All done downloading files/directories!\n"
    end
  end 
end

def getSCPValues(config, account_name)
  scp_user = config.fetch("ssh_user")
  scp_host = config.fetch("export_ssh_host")
  account_dir = File.join(config.fetch("accounts_dir"), account_name)
  return scp_user, scp_host, account_dir
end

if __FILE__ == $PROGRAM_NAME
  
  config_location = getCommandArguments
  config = getConfigFile(config_location)
  
  account_name = config.fetch("account_name")
  
  puts "Exporting data for Account: #{account_name.upcase}"  
  local_tmp_dir = config.fetch("local_tmp_dir")
  
  createTmpDirectory(local_tmp_dir)
  getMongoExportData(config, account_name, local_tmp_dir)
  
  getFileSystemData(config, account_name, local_tmp_dir)
  
  puts "Export Migration Script Finish \n"
  exit 0
end




