# UT Austin Internship
Code written for my summer of 2022 internship at UT Austin (6/22/22 - 7/27/22) with Ph.D student Yefu Chen.

## test1 (6/22/22)
First task given to test abilities: take a large .csv file containing data and restructuring it into a given format. Used [OpenCSV](http://opencsv.sourceforge.net) library. (Note: neither the data or the output are tracked in this repo because their filesizes are too large.)

## dataset1 (6/23/22 - )
Decompressing and restructuring [data](https://app.box.com/folder/112410303785?s=ow0x5ow78ma4hpsrti4wnho8pzdpjrtg) on travel (OD) and stay-at-home dwell time collected every day from 2019-2020 to a better, more useful format. Used [Box SDK Java](http://opensource.box.com/box-java-sdk/) to access Box and [OAuth 2.0](https://developer.box.com/guides/authentication/oauth2/with-sdk/) to authorize Box API. Also going to run same data processing program from `test1` to reformat the csvs in a similar fashion.