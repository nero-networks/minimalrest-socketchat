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
        ul = ele("ul"), inp = ele("input"), users = ele("#users"), status = ele("#status"),
        client = MessageBrokerClient({
            connected: ()=> client.send("SUBSCRIBE", "/chat/"+user),
            status: (txt)=> status.textContent = txt,
            handle: (msg)=> msg.users ? renderUsers(msg) : renderMsg(msg)
        }),
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
        }

    inp.focus()

    fetch("/history")
        .then((res)=> res.json())
        .then((list)=> list.forEach((msg)=> renderMsg(JSON.parse(msg))))

    window.app = {
        send: (text)=> {
            if (text = text || inp.value) {
                client.send("PUBLISH", "/chat/**", {text: text, user: user})
                if (text == inp.value) inp.value = ""
            }
        }
    }

}
