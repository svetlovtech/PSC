# PSC (Proxy Scrapper and Checker)

<img src="https://imgur.com/N0qmKAx.png" width="300"/>

Proxy scrapper and checker from proxyscrape.com

<img src="https://imgur.com/3SUSIrb.gif" width="600"/>

#### Usage
usage: PSC-19.08.jar [-h] [-p <arg>] [-t <arg>]

 -h,--help                     print this message
 
 -p,--thread-pool-size <arg>   thread pool size for checker. Default: 100
 
 -t,--time-out-ms <arg>        get proxies less than %s ms. Default: 1500
 
 Compiled fat jar: <a href="https://drive.google.com/file/d/1lodEyQS6yLZm9BZJH0nq5_V0bB7OOhNw/view?usp=sharing">download JAR from Google drive</a>

#### Results
After the program finishes, 3 files will be created:

badProxies.txt

errorsProxies.txt

goodProxies.txt

#### Example goodProxies.txt:

http://142.93.130.169:8118

http://194.5.159.176:3128

http://194.88.104.62:8888

http://188.166.119.186:80

http://52.157.177.105:80

http://167.71.61.60:8080
