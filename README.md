# UT Austin Internship
Code written for my summer of 2022 internship at UT Austin (6/22/22 - 7/27/22) with Ph.D student Yefu Chen.

## test1 (6/22/22)
First task given to test abilities: take a large .csv file containing data and restructuring it into a given format. Used [OpenCSV](http://opencsv.sourceforge.net) library. (Note: neither the data or the output are tracked in this repo because their filesizes are too large.)

## dataset1 (6/23/22 - )
Step 1: Decompressed [data](https://app.box.com/folder/112410303785?s=ow0x5ow78ma4hpsrti4wnho8pzdpjrtg) on travel (OD) and stay-at-home dwell time collected every day from 2019-2020 to csv files, and restructured data into monthly folders rather than daily, for accessibility. Used [Box SDK Java](http://opensource.box.com/box-java-sdk/) to access Box and [OAuth 2.0](https://developer.box.com/guides/authentication/oauth2/with-sdk/) to authorize Box API.  
Step 2 (WIP): Aggregate device count and destination count data for each month into one file. Problems include `OutOfMemory` errors and algorithms taking too much time. Solution: instead of aggregating each month, choose certain periods of time such as weeks to analyze and aggregate those days only. Using the binary search algorithms, each week only takes ~30 minutes to aggregate.  