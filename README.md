![Visual Studio Code](https://img.shields.io/badge/Visual%20Studio%20Code-0078d7.svg?style=flat&logo=visual-studio-code&logoColor=white) 
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=flat&logo=java&logoColor=white) 
![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=flat&logo=Apache%20Maven&logoColor=white) 
![GitHub release (latest by date)](https://img.shields.io/github/v/release/nmokey/UTAustinInternship?style=flat) 
![GitHub all releases](https://img.shields.io/github/downloads/nmokey/UTAustinInternship/total)
![GitHub](https://img.shields.io/github/license/nmokey/UTAustinInternship) 
# UT Austin Internship
Code written for my summer of 2022 internship at UT Austin (6/22/22 - 7/27/22) with Ph.D student Yefu Chen.

## test1 (6/22/22)
Initial task. Take one day's (08/01/2020) OD data `.csv` file and process it into a given format. Used [OpenCSV](http://opencsv.sourceforge.net) library. *(Note: neither the data or the output are tracked in this repo because their filesizes are too large.)*

## dataset1 (6/23/22 - )
**Step 1:** Decompressed [data](https://app.box.com/folder/112410303785?s=ow0x5ow78ma4hpsrti4wnho8pzdpjrtg) on travel (OD) and stay-at-home dwell time collected every day from 2019-2020 to csv files, and restructured data into monthly folders rather than daily, for accessibility. Used [Box SDK Java](http://opensource.box.com/box-java-sdk/) to access Box and [OAuth 2.0](https://developer.box.com/guides/authentication/oauth2/with-sdk/) to authorize Box API.  
**Step 2:** Process `device_count` and `destination_count` data for the first week of each month into two file, split into weekend and weekdays.  
**Step 3:** Create basic lightweight GUI for more user-friendly interface. App created with Java Swing and can be found in the latest [release](https://github.com/nmokey/UTAustinInternship/releases). Several screenshots attached below.

<img src="https://user-images.githubusercontent.com/77017591/179374762-63f8564b-ec13-4cca-8828-70a246a1c6cb.png" width="45%"></img> 
<img src="https://user-images.githubusercontent.com/77017591/179374628-213fd4d2-5bb2-4506-9941-0da73421d794.png" width="45%"></img> 
<img src="https://user-images.githubusercontent.com/77017591/179374946-acf0c759-ea75-4e35-a716-9b961f80cfb4.png" width="45%"></img> 
<img src="https://user-images.githubusercontent.com/77017591/179374919-cd07452f-c103-4f72-99b8-d6131e0a05c7.png" width="45%"></img> 
<sub> This project is licensed under the terms of the MIT license. </sub>