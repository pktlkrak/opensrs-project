const entryList = document.querySelector("#main-sets-list");

function appendSetEntry(user, entry) {
    const elem = document.createElement("li");
    const a = document.createElement("a");
    if(entry.id) {
        a.href = `/${entry.id}/`;
    }
    a.textContent = entry.name;
    elem.appendChild(a);

    if(entry.id && user.uid != -1) {
        const button = document.createElement("button");
        const icon = document.createElement("img");
        icon.width = 16;
        icon.height = 16;
        button.appendChild(icon);

        if(entry.unlocked) {
            icon.src = "/core-ui/res/trash-solid-full.svg";
            button.title = "{trs:Reset progress}";
            button.onclick = async () => {
                if(!window.confirm(`{trs:Are you sure you want to reset your progress in} '${entry.name}'?`)) {
                    return;
                }
                try {
                    await OSRS.dropSetFor(user.uid, entry.id);
                    window.location.reload();
                } catch(ex) {
                    window.alert(ex.message);
                }
            };
        } else {
            icon.src = "/core-ui/res/play-solid-full.svg";
            button.title = "{trs:Start learning}";
            button.onclick = async () => {
                try {
                    await OSRS.initSetFor(user.uid, entry.id);
                    window.location.reload();
                } catch(ex) {
                    window.alert(ex.message);
                }
            };
        }
        elem.appendChild(button);
    }
    entryList.appendChild(elem);
}

(async () => {
    try {
        const user = await OSRS.getUser();
        const sets = await OSRS.getSets();
        if(user.uid != -1) {
            document.querySelector(".user-thing-login").classList.add("hidden");
            const loggedInButton = document.querySelector(".user-thing-logged-in");
            loggedInButton.querySelector("img").onerror = () => {
                loggedInButton.querySelector("img").src = "core-ui/res/user-regular-full.svg";
            };
            loggedInButton.querySelector("img").src = "/api/user/pic";
            loggedInButton.onclick = () => window.location.href = "profile.html";
            loggedInButton.querySelector("span").textContent = user.username;
            loggedInButton.classList.remove("hidden");
        }
        if(sets.length == 0) {
            appendSetEntry(user, { name: "{trs:No sets loaded. Go install some!}" });
        }
        for(const set of sets) {
            appendSetEntry(user, set);
        }
    } catch(ex) {
        appendSetEntry({ uid: -1 }, { name: `{trs:Failed to load sets:} ${ex.message}` });
    }
})();
