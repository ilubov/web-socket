package cn.ilubov.controller;

import cn.ilubov.server.WebSocketServer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

@RestController
public class SocketController {

    @GetMapping("/index")
    public ModelAndView index() {
        return new ModelAndView("web-socket");
    }

    @GetMapping("/push/{toUserId}")
    public ResponseEntity<String> pushToWeb(@PathVariable String toUserId, String message) throws IOException {
        WebSocketServer.sendInfo(message, toUserId);
        return ResponseEntity.ok("发送成功");
    }
}
