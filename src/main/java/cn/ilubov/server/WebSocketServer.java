package cn.ilubov.server;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@ServerEndpoint("/web-socket/{userId}")
@Component
public class WebSocketServer {

    // 当前在线连接数
    private static final AtomicInteger onlineCount = new AtomicInteger();

    // 存放每个客户端对应的WebSocket对象
    private static final ConcurrentHashMap<String, WebSocketServer> webSocketMap = new ConcurrentHashMap<>();

    // 与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

    // 接收userId
    private String userId = "";

    /**
     * 连接建立成功调用的方法
     *
     * @param session
     * @param userId
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        this.session = session;
        this.userId = userId;
        if (webSocketMap.containsKey(userId)) {
            webSocketMap.remove(userId);
            webSocketMap.put(userId, this);
        } else {
            webSocketMap.put(userId, this);
            // 在线连接数加1
            addOnlineCount();
        }
        log.info("用户连接: {}, 当前在线人数为: {}", userId, getOnlineCount());
        try {
            sendMessage("连接成功");
        } catch (IOException e) {
            log.error("用户: {}, 网络异常", userId);
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        if (webSocketMap.containsKey(userId)) {
            webSocketMap.remove(userId);
            // 在线连接数减1
            subOnlineCount();
        }
        log.info("用户退出: {}, 当前在线人数为: {}", userId, getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message
     * @param session
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("用户消息: {}, 报文: {}", userId, message);
        // 可以群发消息
        // 消息保存到数据库或者redis
        if (StrUtil.isNotBlank(message)) {
            try {
                // 解析发送的报文
                JSONObject jsonObject = JSON.parseObject(message);
                // 加发送人
                jsonObject.put("fromUserId", userId);
                String toUserId = jsonObject.getString("toUserId");
                // 传送给对应toUserId用户的websocket
                if (StrUtil.isNotBlank(toUserId) && webSocketMap.containsKey(toUserId)) {
                    webSocketMap.get(toUserId).sendMessage(jsonObject.toJSONString());
                } else {
                    log.error("请求的userId: {}, 不在该服务器上", toUserId);
                    // 不在这个服务器上，发送到mysql或者redis
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 异常
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("用户错误: {}, 原因: {}", userId, error.getMessage());
        error.printStackTrace();
    }

    /**
     * 服务器主动推送
     *
     * @param message
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    /**
     * 发送自定义消息
     *
     * @param message
     * @param userId
     */
    public static void sendInfo(String message, @PathParam("userId") String userId) throws IOException {
        log.info("发送消息到: {}, 报文: {}", userId, message);
        if (StrUtil.isNotBlank(userId) && webSocketMap.containsKey(userId)) {
            webSocketMap.get(userId).sendMessage(message);
        } else {
            log.error("用户: {}, 不在线", userId);
        }
    }

    /**
     * 获取当前在线连接数
     */
    public static int getOnlineCount() {
        return onlineCount.get();
    }

    /**
     * 增加当前在线连接数
     */
    public static void addOnlineCount() {
        onlineCount.getAndIncrement();
    }

    /**
     * 减少当前在线连接数
     */
    public static void subOnlineCount() {
        onlineCount.getAndDecrement();
    }
}
