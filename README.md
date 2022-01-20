# Server Setup
1. In the server's terminal, switch to be the super user: ```sudo su```.
2. Create a file by ```cat > server_setup.sh```, then copy the contents in ```/server_setup.sh``` in the repository, and paste by ```ctrl + v``` and save by ```ctrl + d```.
3. Run ```bash server_setup.sh``` to setup the environment and install Apache Druid.
4. Execute ```source "/root/.sdkman/bin/sdkman-init.sh"``` again.
5. Change the directory by ```cd DruidDB/apache-druid-0.22.1```.
6. Run the server by ```bin/start-micro-quickstart```. 
7. Now, you can open the console of Apache Druid via ```http://<ip>:8888```. Make sure the protocol is ```http``` instead of ```https```, and port ```8888``` is opened.
8. Open another terminal, run ```wget https://github.com/sen-yigit/TEMGData/raw/main/base-3000-4000.csv```, ```wget https://github.com/sen-yigit/TEMGData/raw/main/price-3000-4000.csv```, and ```wget https://github.com/sen-yigit/TEMGData/raw/main/split-3000-4000.csv``` to download the required data for testing.
9. Ingest data in the Apache Druid console.
  - In the console, click the 'Load data' button.
  - Select 'Local disk' and click 'Connect data'.
  - Specify the 'Base directory' as ```/home/<user_name>/base-3000-4000.csv``` for loading the information of financial instruments. Select '*.csv' for filter and click 'Apply', followed by 'Parse data' to go to the next section.
  - Select 'csv' as 'Import format' and choose 'True' for 'Find columns from header'. Go to the next section (as shown in the sub-header).
  - Choose ```CreateDate``` as ```__time```, and modify the (time) 'Format' to the correct format. Go to the section 'Configure schema'.
  - Make sure the data type of each row is correct. Change by first selecting the column header followed by 'Type' if needed. Go to the next section.
  - Select 'all' for 'Segment granularity'. Go to the section 'Publish'.
  - Type ```base_table``` as the 'Datasource name'. Go to the next section and press 'Submit'.
  - Inspect the time elapsed for ingestion from the tab 'Ingestion'.
  - Wait until the status of the previous task turns 'Success'. Repeat the above step for the remaining tables:
    - Price table storing market data:
      - 'Base directory': ```/home/<user_name>/price-3000-4000.csv```
      - ```time```: ```TradeDate```
      - 'Format': ```yyyy-MM-dd```
      - Data type of 'OpenPrice': 'double'
      - 'Segment granularity': 'day'
      - Datasource name: ```price_table```
    - Split table storing split event:
      - 'Base directory': ```/home/<user_name>/split-3000-4000.csv```
      - 'Format': ```yyyy-MM-dd```
      - 'Segment granularity': 'all'
      - Datasource name: ```split_table```

# Client Setup
1. In the client's terminal, we should update Java to ```Java 17``` and Maven to ```Maven 3.8.X```.
  - First, switch to super user: ```sudo su```.
  - Make sure all existing packages are up-to-date: ```sudo apt update && sudo apt upgrade -y```.
  - Run the following commands to install ```Java 17```: 
    - ```sudo add-apt-repository ppa:linuxuprising/java -y```
    - ```sudo apt update```
    - ```sudo apt-get install -y oracle-java17-installer oracle-java17-set-default```
  - Follow this guide to install Maven 3.8.X: ```https://github.com/m-thirumal/installation_guide/blob/master/maven/upgrade_maven.md```.
2. Change the directory to the home directory ```cd /home/<user_name>```, then clone this repository by ```git clone https://github.com/codermomo/DruidDB_Tester.git```.
3. Change the directory by ```cd DruidDB_Tester```.
4. In ```./src/main/java/Druid_Tester.java```, change ```ip``` (line 5) into the ip address of the server and ```numLoops``` (line 6) into the number of trials you would like to run for the queries. You may first change the directory by ```cd src/main/java```, then open the text editor and edit by ```vim Druid_Tester.java```.
5. In the text editor, press ```i``` to switch to insert mode. Change the parameters in line 5 and line 6. After that, press ```esc``` and ```:wq``` to save the changes.
6. Go back to the root directory of the repository by ```cd ../../..```.
7. Run ```mvn package```, then ```java -Xmx8192m -cp target/Druid_Tester-1.0-SNAPSHOT.jar Druid_Tester``` to execute the java program for testing.
8. A .csv file is generated storing the results.
9. Repeat step 4 to step 7 for different configurations.
