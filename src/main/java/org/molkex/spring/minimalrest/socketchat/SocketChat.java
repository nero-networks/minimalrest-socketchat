package org.molkex.spring.minimalrest.socketchat;

import org.molkex.spring.minimalrest.MinimalMessageBroker;
import org.molkex.spring.minimalrest.MinimalRestApp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@EnableWebSocket
@RestController
@RequestMapping("/chat/")
public class SocketChat extends MinimalMessageBroker {

    protected List<String> history = new ArrayList<>();
    protected String topic = "/chat/";

    @GetMapping("/history")
    public List<String> getHistory() {
        return history;
    }

    @Override
    protected void handle(MinimalMessageBroker.Client client, MinimalMessageBroker.Message message) {
        if (message.getTopic().equals(topic+"**")
                && !message.getPayload().contains("users\":[")) {
            history.add(message.getPayload());
            if (history.size() > 20)
                history.remove(0);
        }
        super.handle(client, message);
    }

    @Override
    protected void afterSubscribe(MinimalMessageBroker.Client client, MinimalMessageBroker.Message message) {
        broadcast();
    }

    @Override
    protected void afterClose(MinimalMessageBroker.Client client) {
        broadcast();
    }

    private void broadcast() {
        publish(topic +"**", new ChatMessage(
                getTopics()
                        .stream()
                        .filter(t -> t.startsWith(topic))
                        .map(t -> t.substring(topic.length()))
                        .collect(Collectors.toSet())));
    }

    public static class ChatMessage {
        public ChatMessage() {}

        ChatMessage(Set<String> users) {
            this.users = users;
        }

        public ChatMessage(String user, String text, boolean direct) {
            this();
            this.user = user;
            this.text = text;
            this.direct = direct;
        }

        public Set<String> getUsers() {
            return users;
        }

        public void setUsers(Set<String> users) {
            this.users = users;
        }

        public boolean direct;
        public Set<String> users;
        public String user;
        public String text;
    }

    public static class ChatBot {

        protected SocketChat chat;
        protected List<String> users = new ArrayList<>();
        protected String topic;
        protected String user;

        public ChatBot(String topic, String user, SocketChat chat) {
            this.chat = chat;
            this.topic = topic;
            this.user = user;

            chat.subscribe(topic+user, ChatMessage.class, m -> {
                if (m.users != null) {
                    onUsers(m);
                } else if (m.direct) {
                    onMessage(m);
                }
            });
        }

        protected void onMessage(ChatMessage chatMessage) {}

        protected void onNewUser(String user) {}

        protected void onUsers(ChatMessage m) {
            Set<String> list = m.users.stream().filter(u -> !u.equals(user)).collect(Collectors.toSet());
            list.stream().filter(u -> !users.contains(u)).forEach(this::onNewUser);

            users.clear();
            users.addAll(list);
        }

        protected void publish(String rcpt, String text) {
            chat.publish(topic+rcpt, new ChatMessage(user, text, !rcpt.endsWith("*")));
        }
    }

    @SpringBootApplication
    public static class App extends MinimalRestApp implements HandlerInterceptor {
        public static void main(String[] args) {
            SpringApplication.run(App.class, args);
        }

        @Bean
        ChatBot socketChatBot(@Autowired SocketChat socketChat) {
            return new ChatBot("/chat/", "bot", socketChat) {
                @Override
                protected void onNewUser(String user) {
                    publish(user,"Hello "+user+",\nwelcome to our chat\nonline are: "+users);
                }

                @Override
                protected void onMessage(ChatMessage msg) {
                    if (msg.text.contains("time"))
                        publish(msg.user, "Hi "+msg.user+"! It is "+ LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))+" now");

                    else
                        publish(msg.user,"Hi "+msg.user+"! Currently... I don't know how to do that, sorry.");
                }
            };
        }
    }
}
