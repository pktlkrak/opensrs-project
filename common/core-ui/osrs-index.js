function showLoading(status) {

}

function showError(ex) {

}

(async () => {
    showLoading(true);
    try{

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

        console.log(user, sets);
        const entryList = document.querySelector("#main-sets-list");
        function appendSetEntry(entry, activatable) {
            const elem = document.createElement("li");
            elem.classList.add("set-entry");
            const a = document.createElement("a");
            if(activatable)
                a.href = `/${entry.id}/`;
            a.textContent = entry.name;
            elem.appendChild(a);
            if(activatable && user.uid != -1) {
                const buttons = document.createElement("span");
                const deleteAllButton = document.createElement("button");
                const icon = document.createElement("img");
                icon.width = 16;
                icon.height = 16;
                icon.src = "core-ui/res/trash-solid-full.svg";
                deleteAllButton.appendChild(icon);
                deleteAllButton.onclick = async () => {
                    if(window.confirm(`Are you sure you want to reset your progress in '${entry.name}'?`)) {
                        await OSRS.dropSetFor(user.uid, entry.id);
                        window.location.reload();
                    }
                }
                buttons.appendChild(deleteAllButton);
                elem.appendChild(buttons);
            }
            entryList.appendChild(elem);
        }
        if(sets.length == 0) {
            appendSetEntry({ id: '', name: "No sets loaded. Go install some!" }, false); 
        } else {
            for(let x of sets) {
                appendSetEntry(x, true);
            }
        }
    } catch(ex) {
        showLoading(false);
        showError(ex);
    }
})();