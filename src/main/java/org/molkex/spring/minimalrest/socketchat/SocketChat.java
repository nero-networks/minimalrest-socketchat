package org.molkex.spring.minimalrest.socketchat;

import org.molkex.spring.minimalrest.MinimalMessageBroker;
import org.molkex.spring.minimalrest.MinimalRestApp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@EnableWebSocket
@RestController
@RequestMapping("/")
public class SocketChat extends MinimalMessageBroker {
    private static final String TOPIC = "/chat/**";

    List<String> history = new ArrayList<>();

    @GetMapping("/history")
    public List<String> getHistory() {
        return history;
    }

    @Override
    protected void handle(Client client, Message message) {
        if (TOPIC.equals(message.getTopic()) && !message.getPayload().contains("users")) {
            history.add(message.getPayload());
            if (history.size() > 20)
                history.remove(0);
        }
        super.handle(client, message);
    }

    @Override
    protected void afterSubscribe(Client client, Message message) {
        broadcast();
    }

    @Override
    protected void afterClose(Client client) {
        broadcast();
    }

    private void broadcast() {
        publish("/chat/**", new UsersBroadcast(
                getTopics()
                        .stream()
                        .filter(t -> t.startsWith("/chat/"))
                        .map(t -> t.substring(6))
                        .collect(Collectors.toSet())));
    }

    static class UsersBroadcast {
        private Set<String> users;

        UsersBroadcast(Set<String> users) {
            this.users = users;
        }

        public Set<String> getUsers() {
            return users;
        }

        public void setUsers(Set<String> users) {
            this.users = users;
        }
    }

    @SpringBootApplication
    public static class App extends MinimalRestApp {
        public static void main(String[] args) {
            SpringApplication.run(App.class, args);
        }
    }
}
