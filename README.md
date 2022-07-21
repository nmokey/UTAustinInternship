[![Visual Studio Code](https://img.shields.io/badge/Visual%20Studio%20Code-0078d7.svg?style=for-the-badge&logo=visual-studio-code&logoColor=white)](https://code.visualstudio.com)
[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)](https://openjdk.org/)
[![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white)](https://maven.apache.org)
[![Developer](https://img.shields.io/badge/developer-nmokey-orange?style=for-the-badge)](https://github.com/nmokey)

[![GitHub release (latest by date)](https://img.shields.io/github/v/release/nmokey/UTAustinInternship?style=flat)](https://github.com/nmokey/UTAustinInternship/releases)
[![GitHub all releases](https://img.shields.io/github/downloads/nmokey/UTAustinInternship/total)](https://github.com/nmokey/UTAustinInternship/releases)
![Man Hours](https://img.shields.io/endpoint?url=https%3A%2F%2Fmh.jessemillar.com%2Fhours%3Frepo%3Dhttps%3A%2F%2Fgithub.com%2Fnmokey%2FUTAustinInternship.git)
![HitCount](https://hits.dwyl.com/nmokey/UTAustinInternship.svg?style=flat)
[![GitHub license](https://img.shields.io/github/license/nmokey/UTAustinInternship)](https://github.com/nmokey/UTAustinInternship/blob/main/LICENSE)
# UT Austin Internship
Code written for my summer of 2022 internship at UT Austin (6/22/22 - 7/27/22) with Ph.D student Yefu Chen.  

> [UT Austin Internship](#ut-austin-internship)
>>[test1](#test1-62222)  
>>[dataset1](#dataset1-62322---71922)  

>[How to use the Data Manager app](#how-to-use-the-data-manager-app)  
>>[File Organizer](#file-organizer)  
>>[File Processor](#file-processor)  
>>[Data Aggregator](#data-aggregator)  
>>[Report Issues](#report-issues)  
## test1 (6/22/22)
Initial task. Take one day's (08/01/2020) OD data `.csv` file and process it into a given format. Used [OpenCSV](http://opencsv.sourceforge.net) library. *(Note: neither the data or the output are tracked in this repo because their filesizes are too large.)*

## dataset1 (6/23/22 - 7/19/22)
**Step 1:** Decompress [data](https://app.box.com/folder/112410303785?s=ow0x5ow78ma4hpsrti4wnho8pzdpjrtg) on travel (OD) and stay-at-home dwell time collected every day from 2019-2020 to csv files, and restructure into monthly folders rather than daily for accessibility. Used [Box SDK Java](http://opensource.box.com/box-java-sdk/) to access Box and [OAuth 2.0](https://developer.box.com/guides/authentication/oauth2/with-sdk/) to authorize Box API.  
**Step 2:** Process `device_count` and `destination_count` data for the first week of each month into two file, split into weekend and weekdays.  
**Step 3:** Create basic lightweight GUI for more user-friendly interface. App created with Java Swing and can be found in the latest [release](https://github.com/nmokey/UTAustinInternship/releases). Several screenshots attached below.

<img src="https://user-images.githubusercontent.com/77017591/179374762-63f8564b-ec13-4cca-8828-70a246a1c6cb.png" width="45%"></img> 
<img src="https://user-images.githubusercontent.com/77017591/179374628-213fd4d2-5bb2-4506-9941-0da73421d794.png" width="45%"></img> 
<img src="https://user-images.githubusercontent.com/77017591/179374946-acf0c759-ea75-4e35-a716-9b961f80cfb4.png" width="45%"></img> 
<img src="https://user-images.githubusercontent.com/77017591/179374919-cd07452f-c103-4f72-99b8-d6131e0a05c7.png" width="45%"></img>  

**Step 4:** Add `FileAggregator` functionality to app: detect processed files and aggregate each origin's information into one row. See below for example:

| Data Status | Example | Includes |
| ----------- | ------- | -------- |
| Raw data (~400 MB/day) | <img width="1340" alt="image" src="https://user-images.githubusercontent.com/77017591/180106729-759b5b9f-2207-450f-a8fb-8329b0c6318c.png"> | <ul><li>-[x] Origin CBG</li><li>-[x] Destination CBG</li><li>- [ ] Origin count</li><li>- [ ] Destination count</li><li>- [ ] Sociodemographic data</li><li>- [x] Other data (not shown)</li></ul> |
| Processed data (~300 MB/day) | <img width="1341" alt="image" src="https://user-images.githubusercontent.com/77017591/180107471-c471c904-cf39-4ea8-87dc-a89e77eaa2ee.png"> | <ul><li>-[x] Origin CBG</li><li>-[x] Destination CBG</li><li>- [ ] Origin count</li><li>- [x] Destination count</li><li>- [ ] Sociodemographic data</li><li>- [ ] Other data</li></ul> |
| Aggregated data (~37 MB always) | <img width="1342" alt="image" src="https://user-images.githubusercontent.com/77017591/180106923-6a4a0cab-b505-4a6b-8cc7-59bd36a8b8c8.png"> | <ul><li>-[x] Origin CBG</li><li>-[ ] Destination CBG</li><li>- [x] Origin count</li><li>- [x] Destination count</li><li>- [x] Sociodemographic data</li><li>- [ ] Other data</li></ul> |
</div>
In the aggregated file, <code>origin_count</code> represents the number of devices that left the Census Block Group (CBG) in that time period, and <code>destination_count</code> represents the number of devices that arrived in the CBG. <code>device_count</code> represents the total number of devices registered to the CBG.  

[Back to top](#ut-austin-internship)

# How to use the Data Manager app  
To get the app, find and download the [latest release](https://github.com/nmokey/UTAustinInternship/releases).  
### How to not break the app:
- The Box account you login with must have access to the folder "daily-social-distancing-v2".
- The folders, including "daily-social-distancing-v2" and all subfolders, must not be renamed.
- The File Processor's existing output filenames must not be changed, but you can add to the end for clarification (e.g. "weekend, weekdays, fullweek")
- The File Aggregator must not try to aggregate a month folder that already has aggregated files in it.  
If you meet all of these conditions and the app still somehow breaks or doesn't work, please [report an issue](#report-issues).
## File Organizer
The file organizer button's function was to unzip each .gz archive in the dataset, but as that is already done, this button should not be used anymore.
## File Processor
To process a range of files, input the specified date range into the dialoggue box that pops up. Next, you should be redirected to a Box Authorization page. Log in to Box and click `Grant Access to Box`, which should redirect you to [google.com](google.com). Copy the authcode from the URL (`https://www.google.com/?code=AUTHCODEISHERE`) and paste it into the next dialogue box. You only need to be authorized once each time you open the app.  
Once you have provided all the input, the app will begin. The process should take no more than ~7 minutes per file on average. Once all the files have been processed the app will write the output file to your Desktop folder in the format `YEAR_STARTDATE_ENDDATE.csv`. This file should then be uploaded to the appropriate Box month folder for aggreagtion.
## Data Aggregator
The data aggregator aggregates one month at a time, so input the month you want to aggregate. Then, follow the steps detailed above if the authorization dialogue pops up. Once aggregation is complete, the app will upload the output files to the appropriate Box month folder and no further action is required from you.
## Report Issues
Are you experiencing issues when using the app? (e.g. app unresponsiveness or File Processor/Data Aggregator not doing anything) Please submit an [issue](https://github.com/nmokey/UTAustinInternship/issues/new) and I will try to fix it as soon as possible. Include all information about which function you were using at the time, how the app is not working, etc. Thanks!

[Back to top](#ut-austin-internship)

<hr/>

This project is licensed under the terms of the [MIT License](https://github.com/nmokey/UTAustinInternship/blob/main/LICENSE).
