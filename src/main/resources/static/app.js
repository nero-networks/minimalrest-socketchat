window.onload = ()=> {
    var ele = (s, p)=> (p||document).querySelector(s),
        topic = "/chat/",
        user = minimalrest.queryPrompt("Enter user name"),
        root = ele(".socket-chat"),
        client = minimalrest.MessageBrokerClient({
            connected: ()=> client.send("SUBSCRIBE", topic+user),
            status: (txt)=> status.textContent = txt,
            handle: (msg)=> msg.users ? renderUsers(msg) : renderMsg(msg)
        }),
        channel = "",
        ul = ele(".list", root),
        inp = ele(".input", root),
        users = ele(".users", root),
        status = ele(".status", root),

        renderMsg = (msg)=> {
            var li = document.createElement("li")
            li.innerHTML = "<pre><strong></strong>: </pre>"

            ele("pre strong", li).textContent =
                !msg.direct ? msg.user : msg.user == user ? user+channel : msg.user+"@"+user

            ele("pre", li).appendChild(document.createTextNode(msg.text))

            ul.appendChild(li)
            li.scrollIntoView()
        },
        renderUsers = (msg)=> {
            users.textContent = msg.users.filter((u)=> u != user).join(", ")
        },
        send = (text)=> {
            if (text = text || inp.value) {
                var rcpt = "**", msg = text, m, data = {user: user}
                channel = ""
                if (m = text.match(/^@([^: ]*)?/)) {
                    rcpt = m[1]
                    channel = "@"+rcpt+" "
                    msg = msg.substr(rcpt.length+1).trim()
                    data.direct = !channel.endsWith("*")
                }
                data.text = msg
                if (rcpt && msg) {
                    if (data.direct) renderMsg(data)
                    client.send("PUBLISH", topic+rcpt, data)
                }
                if (text == inp.value) inp.value = channel
            }
        }

    inp.focus()
    inp.onkeypress = (event)=> {event.which == 13 && send()}

    fetch(topic+"history")
        .then((res)=> res.json())
        .then((list)=> list.forEach((msg)=> renderMsg(JSON.parse(msg))))
}