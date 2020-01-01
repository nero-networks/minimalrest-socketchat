window.onload = ()=> {
    var user;

    while (!location.search) {
        if (user = prompt("Enter user name")) {
            location.assign(location.href+'?'+user)
            return
        }
    }

    user = location.search.substr(1)

    var ele = (s, p)=> (p||document).querySelector(s),
        ul = ele("ul"), inp = ele("input"), content = ele("#content"), users = ele("#users"), status = ele("#status"),
        handle = (msg)=> {
            msg.users ? renderUsers(msg) : renderMsg(msg)
        },
        renderUsers = (msg)=> {
            users.textContent = msg.users.filter((u)=> u != user).join(", ")
        },
        renderMsg = (msg)=> {
            var li = document.createElement("li")
            li.innerHTML = "<pre><strong></strong>: </pre>"

            ele("pre strong", li).textContent = msg.user
            ele("pre", li).appendChild(document.createTextNode(msg.text))

            ul.appendChild(li)
            li.scrollIntoView()
        },

        client = (()=> {
            var socket,
                reconnects = 0,
                scheduled,        
                schedule = (millis)=> {
                    if (!scheduled ) {
                        scheduled = true
                        setTimeout(connect, millis)
                        if (millis > 3000)
                            tick(new Date(+new Date() + millis))
                    }
                },
                tick = (until)=> {
                    var seconds = Math.round((+until - +new Date()) / 1000);
                    status.textContent = "reconnect attempt " + reconnects + " in " + seconds + " seconds"
                    if (seconds > 0) setTimeout(()=> tick(until), 1000)
                },
                connect = ()=> {
                    scheduled = false
                    if (reconnects > 90) reconnects = 30

                    socket = new WebSocket("ws"+location.protocol.substr(4)+"//"+location.host+"/broker")
                    socket.onmessage = (m)=> handle(JSON.parse(JSON.parse(m.data).payload))
                    socket.onclose = ()=> schedule(500)
                    socket.onerror = ()=> schedule(2000 * ++reconnects)
                    socket.onopen = ()=> {
                        reconnects = 0
                        send("SUBSCRIBE", "/chat/"+user)
                        status.textContent = 'online'
                    }
                },
                send = (type, topic, payload)=> {
                    if (payload) payload.user = user
                    socket.send(JSON.stringify({type:type, topic:topic, payload: payload ? JSON.stringify(payload) : undefined}))
                }
            connect()
            return {
                send: send
            }
        })()

    inp.focus()

    fetch("/history")
        .then((res)=> res.json())
        .then((list)=> list.forEach((msg)=> renderMsg(JSON.parse(msg))))

    window.app = {
        send: (text)=> {
            if (text = text || inp.value) {
                client.send("PUBLISH", "/chat/**", {text: text})
                if (text == inp.value) inp.value = ""
            }
        }
    }

}
