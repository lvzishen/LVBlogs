#### 目录介绍

- 1.Http是什么
-  2.Http请求格式
- 3.Http的报文格式
    - 3.1 请求
    - 3.2 响应
- 4.请求方法：列举常见的5种
    - 4.1 GET
    - 4.2 POST
    - 4.3 PUT
    - 4.4 DELETE
    - 4.5 HEAD
- 5.Status Code状态码
- 6.Header
- 7.RESTful是什么
- 8.HTTP1.0 HTTP 1.1主要区别
- 9.HTTP1.1 HTTP 2.0主要区别




### 一.Http是什么？
中文名称--超文本传输协议，是TCP/IP协议族的最顶层-**应用层**。应用层协议的产生只是为了C/S双方都能看懂数据，是对传输数据的包装。
### 二.Http请求格式
URL格式分为三部分：
协议类型://服务器地址(和端口号)/路径(Path)
https://segmentfault.com/write?freshman=1
### 三.Http的报文格式
#### 1.请求：


![](https://user-gold-cdn.xitu.io/2019/3/12/16972310af05866d?w=609&h=401&f=png&s=83725)

 - 请求报文---->分为 : 请求行，Header，Body三部分。   
 -  请求行中包括请求的方式（GET,POST等），请求的路径（主机地址之后的部分），Http的版本号   
 -  请求头包括一些上传信息的属性，是固定名称的键值对形式，最后一个请求头之后是一个空行，这个行非常重要，它表示请求头已经结束，接下来的是请求正文。   
 -  请求体是上传的内容，一般配合请求头Content-Type为 x-www-form-urlencoded的形式使用（表单格式），GET请求没有请求体。
#### 2.响应：


![](https://user-gold-cdn.xitu.io/2019/3/12/169723152f0516cd?w=606&h=276&f=png&s=78342)
- 响应报文---->分为：响应行，Header，Body三部分。
- 响应行中包括Http版本号，响应状态码和响应状态信息三部分。
- 响应体根据服务器返回的数据格式为主，一般响应体都是返回JSON格式。

### 四.请求方法：列举常见的5种
##### 理论上讲 GET POST 除了这两个词不一样之外没有任何区别。区别都是客户端的约定俗成,以下均是在HTTP以标准规范使用的情况下为基础。
#### **GET**
- 用于直接从服务器上获取资源。
- 对服务器数据不进行修改，满足幂等性（反复调用多次时会得到相同的结果。例如执行十次相同的GET请求相当于从服务器获取十次数据，但并不会对服务器数据造成修改）。
不发送Body请求体。

```
GET  /users/1  HTTP/1.1 

Host: api.github.com
```
#### **POST**
- 用于增加或修改服务器上的资源。
- 发送给服务器的内容写在Body中。
- 不满足幂等性（增加资源的时候执行一次和执行十次不同，执行一次增加一个，执行十次增加十个）。
```
POST /users HTTP/1.1    
                                               
Host: api.github.com 
Content-Type: application/x-www-form-urlencoded  （Content-Type为 “application/x-www-form-urlencoded”时，才会读取Body中的内容）
Content-Length: 13 

name=lvzishen&gender=male     
```
#### **PUT**
- 用于修改服务器上的资源（和POST请求功能有些重复了，POST既能新增又能修改资源，PUT只能修改资源）。
- 发送给服务器的内容写在Body中。
- 满足幂等性。

```
PUT /users HTTP/1.1            
                                       
Host: api.github.com 
Content-Type: application/x-www-form-urlencoded  （Content-Type为 “application/x-www-form-urlencoded”时，才会读取Body中的内容）
Content-Length: 13 

name=lvzishen&gender=male
```
#### **DELETE**
- 用于删除服务器上的资源。
- 不发送BODY。


```
DELETE /users/1 HTTP/1.1 

Host: api.github.com
```
#### **HEAD**
- 和GET请求一样都是从获取资源（**和GET请求不同的是响应数据中没有BODY**）。
- 一般可以用于下载文件，比如先用HEAD请求去看是否支持服务器是否支持分段下载功能，如果支持再用GET或POST请求配置相应分段传输的数据去下载获取资源，避免无用的资源浪费。检查资源的有效性（如先用HEAD拉去获取ACCEPT RANGE总长度，再用GET请求配合RANGE去下载部分内容）。



```
HEAD  /users/1  HTTP/1.1 

Host: api.github.com
```
### 五.Status Code状态码
用于对相应结果作出类型化描述。
- 1xx：临时性消息。如：100 （继续发送）、101（正在切换协议）
- 2xx：成功。最典型的是 200（OK）、201（创建成功）。 
- 3xx：重定向。如 301（永久移动）、302（暂时移动）、304（内容未改变）。 301表示搜索引擎在抓取新内容的同时也将旧的网址交换为重定向之后的网址；302表示旧地址A的资源还在（仍然可以访问），这个重定向只是临时地从旧地址A跳转到地址B，搜索引擎会抓取新的内容而保存旧的网址(浏览器缓存的地址)。
- 4xx：客户端错误。如 400（客户端请求错误）、401（认证失败）、403（被禁止）、404（找不到内容）。  401配合Authorization,403比如IP被禁止,404比如传的URL错误
- 5xx：服务器错误。如 500（服务器内部错误）。

### 六.Header 

1.Header是什么？
HTTP消息的 **`metadata`**（元数据）。   

2.什么是元数据？
**`通俗的讲就是数据的属性`**。以一个登录接口为例，用户名密码是上传的数据（放在BODY中），那么用户名密码的长度（ Content-Length）、以什么类型( Content-Type )上传等等就是它的元数据。

#### **几个重要的Header**：

#### **1.Host** 
目标主机。注意：不是在网络上用于寻址（寻址用DNS）的，而是在目标服务器(找到IP后一个IP下有可能有多个主机名，确定访问的是哪一个主机)上用于定位子服务器的。

#### 2.**Content-Type**
##### 指定Body的类型。主要分为四类：

(1). **text/html**： 请求 Web 页面是返回响应的类型，Body 中返回 html文本。格式如下：

```
HTTP/1.1 200 OK 

Content-Type: text/html;charset=utf-8 
Content-Length: 853 ......

<!DOCTYPE html>
<html>
<head>
     <meta charset="utf-8">
.....
```
(2). **x-www-form-urlencoded** : 纯文本表单的提交方式（**只有以`表单`形式提交时，才会读取Body中的内容**）。 格式如下：

```
POST /users HTTP/1.1

Host: api.github.com 
Content-Type: application/x-www-form-urlencoded 
Content-Length: 27 

name=lvzishen&gender=male
```
对应 Retrofit 的代码：

```
@FormUrlEncoded      <--代表的是以普通表单（application/x-www-form-urlencoded）上传文本参数,加了这个注解后才会读取@Field中的参数,因为@Field最后会转变为请求体Body中的内容.
@POST("/users") 
Call addUser(@Field("name") String name, @Field("gender") String gender);
```

(3).**multipart/form-data** ：页面含有**二进制文件时的提交方式(上传文件或文字都可以，但是一般没有用此上传纯文字，因为会多加如boundary、分隔符等字符，浪费流量和带宽)。**（只有以表单形式提交时，才会读取Body中的内容）。 格式如下：

```
POST /users HTTP/1.1 
Host: hencoder.com 
Content-Type: multipart/form-data; boundary=----        boundary为起始点
WebKitFormBoundary7MA4YWxkTrZu0gW 
Content-Length: 2382 

------WebKitFormBoundary7MA4YWxkTrZu0gW                 WebKitFormBoundary7MA4YWxkTrZu0gW<--为分隔符
Content-Disposition: form-data; name="name" 

rengwuxian 
------WebKitFormBoundary7MA4YWxkTrZu0gW 
Content-Disposition: form-data; name="avatar"; filename="avatar.jpg" 
Content-Type: image/jpeg 

JFIFHHvOwX9jximQrWa......
```
(4). **application/json , image/jpeg , application/zip ...**

单项内容（文本或非文本都可以），用于 Web Api 的响应或者 POST / PUT 的请求
请求中提交 JSON：

```
POST /users HTTP/1.1

Host: hencoder.com 
Content-Type: application/json; charset=utf-8 
Content-Length: 38 

{"name":"lvzishen","gender":"male"}
```
响应中返回JSON：
```
HTTP/1.1 200 OK

content-type: application/json; charset=utf-8 
content-length: 234

[{"login":"mojombo","id":1,"node_id":"MDQ6VXNl cjE=","avatar_url":"https://avatars0.githubuse rcontent.com/u/1?v=4","gravat......
```
image/jpeg：提交或获取图片
```
POST /user/1/avatar HTTP/1.1 

Host: hencoder.com 
Content-Type: image/jpeg 
Content-Length: 1575 

JFIFHH9......
```
#### **3.Content-Length**
指定 Body 的长度（字节）。用于二进制文件中的长度读取，因为不能确定读取到哪里，所以规定长度，读取到对应长度后代表读取完成不再读取。

#### **4.Transfer: chunked (分块传输编码 Chunked Transfer Encoding)**
用于当响应发起时，内容长度还没能确定的情况下。和 Content-Length 不同时使用。用途是尽早给出响应，减少用户等待。

```
HTTP/1.1 200 OK 

Content-Type: text/html 
Transfer-Encoding: chunked

4 
Chun                  <--不断给出数据 先给4字节的Chun,再给9字节的ked Trans,.....,0代表传输完成全部加载完毕。
9 
ked Trans 
12 
fer Encoding 
0
```
#### **5.Location**
指定重定向的目标的URL

#### **6.User-Agent**
用户代理，即是谁实际发送请求、接受响应的，例如手机浏览器、某款手机 App。

#### **7.Range / Accept-Range**
按范围取数据
- Accept-Range: bytes 响应报文中出现，表示服务器支持按字节来取范围数据
- Range: bytes =<start>-<end>  请求报文中出现，表示要取哪段数据
- Content-Range:<start>-<end>/total  响应报文中出现，表示发送的是哪段数据
作用：断点续传、多线程下载。

#### **8.Cache**
作用：在客户端或中间网络节点缓存数据，降低从服务器取数据的频率，以提高网络性能。
#### **9.Cookie**



![](https://user-gold-cdn.xitu.io/2019/3/12/1697235aff674cf0?w=553&h=342&f=png&s=59160)


![](https://user-gold-cdn.xitu.io/2019/3/12/1697235e01e2b8dd?w=550&h=127&f=png&s=19412)

 1. 服务器需要客户端保存的内容，放在 Set-Cookie headers 里返回，客户端会自动保存。 
 2. 客户端保存的 Cookies，会在之后的所有请求中都携带进 Cookie header 里发回给服务 器。 
 3. 客户端保存 Cookie 是按照服务器域名来分类的，例如 shop.com 发回的 Cookie 保存下来 以后，在之后向 games.com 的请求中并不会携带。
 4. 客户端保存的 Cookie 在超时后会被删除、没有设置超时时间的 Cookie （称作 Session Cookie）在浏览器关闭后就会自动删除；另外，服务器也可以主动删除还未过期的客户端 Cookies。
#### **10.Authorization**
##### **(1)Basic**
**格式**：Authorization: Basic username:password(Base64ed)               对用户名密码做Base64编码，不是加密的，Base64只是编码。
           Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==

##### **(2)Bearer**
**格式**：Authorization: Bearer token
bearer token 的获取方式：
通过 OAuth2 的授权流程 OAuth2 的流程

![](https://user-gold-cdn.xitu.io/2019/3/12/16972367ddbba6c3?w=522&h=531&f=png&s=49282)
0. 第三方网站向授权授权方网站申请第三方授权合作，拿到 client id 和 client secret 
1. 用户在使用第三方网站时，点击「通过 XX (如 GitHub) 授权」按钮，第三方网站将跳转到授权网站，并传入 client id 作为自己的身份标识 
2. 授权方网站根据 client id ，将第三方网站的信息和第三方网站需要的用户权限展示给用户，并询问用户是否同意授权 
3. 用户点击「同意授权」按钮后，授权方网站将跳转回第三方网站，并传入 Authorization code 作为用户认可的凭证。 
4. 第三方网站将 Authorization code 发送回自己的服务器 
5. 服务器将 Authorization code 和自己的 client secret 一并发送给授权方的服务器，授权方 服务器在验证通过后，返回 access token。OAuth 流程结束。
6. 在上面的过程结束之后，第三方网站的服务器（或者有时客户端也会）就可以使用 access token 作为用户授权的令牌，向授权方网站发送请求来获取用户信息或操作用户账户。但这已经在 OAuth 流程之外。

##### 为什么 OAuth 要引入Authorization code，并需要申请授权的第三方将 Authorization code 发送回自己的服务器，再从服务器来获取 access token，而不是直接返回 access token ？这样复杂的流程意义何在？ 
答:为了安全。OAuth 不强制授权流程必须使用 HTTPS，因此需要保证当通信路径中存在窃听者时，依然具有足够的安全性。

---

在自家 App 中使用 Bearer token 
有的 App 会在 Api 的设计中，将登录和授权设计成类似 OAuth2 的过程，但简化掉 Authorization code 概念。即：登录接收请求成功时，会返回 access token，然后客户端在之 后的请求中，就可以使用这个 access token 来当做 bearer token 进行用户操作了。 (这么做失去了使用OAuth2的意义)

Refresh token 用法：access token 有失效时间，在它失效后，调用 refresh token 接口，传入refresh_token 来获取新的 access token。

```
{
"token_type": "Bearer", 
"access_token": "xxxxx", 
"refresh_token": "xxxxx", 
"expires_time": "xxxxx" 
}
```
目的：安全。
当 access token 失窃时，由于它有失效时间，因此坏人只有较短的时间来「做坏事」；同时，由于（在标准的 OAuth2 流程中）refresh token 永远只存在与第三方服务的服务 器中，因此 refresh token 几乎没有失窃的风险。
###七.RESTful是什么？
以HTTP标准协议去使用HTTP。
比如:
1. 规范地使用method 来定义网络请求操作（删除数据用DELETE请求，而不是用POST。获取数据不要使用POST请求而应该使用GET请求）。
2. 规范地使用 status code 来表示响应状态。

### 八.HTTP1.0 HTTP 1.1主要区别
#### **一.长连接 Keep-Alive**
HTTP 1.0需要使用keep-alive参数来告知服务器端要建立一个长连接，而HTTP1.1默认**支持长连接**。Connection:keep-alive（**虽然是不断开但是每次也只能串行的执行HTTP请求，2.0才支持并行**）
HTTP是基于TCP/IP协议的，创建一个TCP连接是需要经过三次握手的,有一定的开销，如果每次通讯都要重新建立连接的话，对性能有影响。因此最好能维持一个长连接，可以用个长连接来发多个请求。
持久连接的时间参数，通常由服务器设定，比如 nginx 的 **`keepalivetimeout，keepalive timout`** 时间值意味着：一个 http 产生的 tcp 连接在传送完最后一个响应后，还需要 hold 住 keepalive_timeout (通常为5-15S)秒后，才开始关闭这个连接；
#### **二.节约带宽**
HTTP 1.1支持只发送header信息(不带任何body信息)，如果服务器认为客户端有权限请求服务器，则返回100，否则返回401。客户端如果接受到100，才开始把请求body发送到服务器。
这样当服务器返回401的时候，客户端就可以不用发送请求body了，节约了带宽。
另外HTTP还支持传送内容的一部分。这样当客户端已经有一部分的资源后，只需要跟服务器请求另外的部分资源即可。这是支持文件断点续传的基础。
#### **三.HOST域**
 现在可以web server例如tomat，设置虚拟站点是非常常见的，也即是说，web server上的多个虚拟站点可以共享同一个ip和端口。
HTTP1.0是没有host域的，HTTP1.1才支持这个参数。

### 九.HTTP1.1 HTTP 2.0主要区别
#### **一.多路复用的单一长链接**
##### **1.单一长连接**
在HTTP/2中，客户端向某个域名的服务器请求页面的过程中，只会创建一条TCP连接，即使这页面可能包含上百个资源。 单一的连接应该是HTTP2的主要优势，单一的连接能减少TCP握手带来的时延 。HTTP2中用一条单一的长连接，避免了创建多个TCP连接带来的网络开销，提高了吞吐量。
##### **2.多路复用**
HTTP2虽然只有一条TCP连接，但是在逻辑上分成了很多stream。
HTTP2把要传输的信息分割成一个个二进制帧，首部信息会被封装到HEADER Frame，相应的request body就放到DATA Frame,一个帧你可以看成路上的一辆车,只要给这些车编号，让1号车都走1号门出，2号车都走2号门出，就把不同的http请求或者响应区分开来了。但是，这里要求同一个请求或者响应的帧必须是有有序的，要保证FIFO的，但是不同的请求或者响应帧可以互相穿插。这就是HTTP2的多路复用，是不是充分利用了网络带宽，是不是提高了并发度？


![](https://user-gold-cdn.xitu.io/2019/3/12/1697237561d85e95?w=720&h=714&f=png&s=198385)
#### **二.Header数据压缩**
HTTP1.1不支持header数据的压缩，HTTP2.0使用HPACK算法对header的数据进行压缩，这样数据体积小了，在网络上传输就会更快。
**相当于建立一个映射表，如GET方法直接定义为映射表中的数字2，那么GET请求时请求方法写2就可以而不用写GET，这样就可以节省流量和空间。**
#### **三.服务器推送**
这个功能通常被称作“**缓存推送**”。主要的思想是：当一个客户端请求资源X，而服务器知道它很可能也需要资源Z的情况下，服务器可以在客户端发送请求前，主动将资源Z推送给客户端，这样当使用Z资源时就不用请求网络直接本地取数据就可以了，加快获取速度。








