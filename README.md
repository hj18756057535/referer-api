# referer-api
百度referer api 获取百度检索关键词


yum安装nginx的路径

(1) Nginx配置路径：/etc/nginx/ 

(2) PID目录：/var/run/nginx.pid 

(3) 错误日志：/var/log/nginx/error.log 

(4) 访问日志：/var/log/nginx/access.log 

(5) 默认站点目录：/usr/share/nginx/html

    location / {
            proxy_pass https://referer.bj.baidubce.com;
    }
使用百度云服务器nginx转发  客户端调用分解压力
最大并发   15QPS 
单日上限不清楚