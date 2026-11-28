const setsList = document.querySelector("#main-sets-list");
const picker = document.querySelector("#image-picker");
const tiles = document.querySelector("#image-tiles");
const loadMoreButton = document.querySelector("#load-more-images");
const dropZone = document.querySelector("#drop-zone");
const fileInput = document.querySelector("#image-upload");
const passwordForm = document.querySelector("#password-form");
const passwordStatus = document.querySelector("#password-status");

let user = null;
let selectedSetId = null;
let loadedImages = 0;

function appendSetEntry(entry) {
    const elem = document.createElement("li");
    const a = document.createElement("a");
    a.href = "#";
    a.textContent = entry.name;
    elem.appendChild(a);

    const img = document.createElement("img");
    img.width = 32;
    img.height = 32;
    img.onerror = () => img.src = "/core-ui/res/user-regular-full.svg";
    img.src = `/api/user/pic?set=${encodeURIComponent(entry.id)}`;
    elem.appendChild(img);

    elem.onclick = event => {
        event.preventDefault();
        if(selectedSetId) {
            selected.elem.classList.remove("selected");
        }
        selectedSetId = entry.id;

        elem.classList.add("selected");
        picker.classList.remove("hidden");
        picker.scrollIntoView({ block: "nearest" });
        if(loadedImages == 0) {
            loadImages();
        }
    };
    setsList.appendChild(elem);
}

async function loadImages() {
    try {
        const result = await OSRS.listImages(loadedImages);
        for(const image of result.images) {
            const img = document.createElement("img");
            img.src = `/api/images/${image.id}`;
            img.onclick = () => assignImage(image.id);
            tiles.appendChild(img);
        }
        loadedImages += result.images.length;
        if(result.hasMore) {
            loadMoreButton.classList.remove("hidden");
        } else {
            loadMoreButton.classList.add("hidden");
        }
    } catch(ex) {
        window.alert(ex.message);
    }
}

async function assignImage(imageId) {
    try {
        await OSRS.assignImage(selectedSetId, imageId);
        window.location.reload();
    } catch(ex) {
        window.alert(ex.message);
    }
}

async function uploadImage(file) {
    if(!file.type.startsWith("image/")) {
        window.alert("{trs:Only images can be uploaded.}");
        return;
    }
    try {
        await assignImage((await OSRS.uploadImage(file)).id);
    } catch(ex) {
        window.alert(ex.message);
    }
}

loadMoreButton.onclick = () => loadImages();

fileInput.onchange = () => {
    if(fileInput.files.length > 0) {
        uploadImage(fileInput.files[0]);
    }
    fileInput.value = "";
};

dropZone.ondragover = event => {
    event.preventDefault();
    dropZone.classList.add("drag-over");
};

dropZone.ondragleave = () => dropZone.classList.remove("drag-over");

dropZone.ondrop = event => {
    event.preventDefault();
    dropZone.classList.remove("drag-over");
    if(event.dataTransfer.files.length > 0) {
        uploadImage(event.dataTransfer.files[0]);
    }
};

passwordForm.onsubmit = async event => {
    event.preventDefault();
    const data = new FormData(passwordForm);
    if(data.get("password") !== data.get("repeat")) {
        passwordStatus.textContent = "{trs:The new passwords do not match.}";
        return;
    }
    try {
        await OSRS.changePassword(user.username, data.get("current"), data.get("password"));
        passwordForm.reset();
        passwordStatus.textContent = "{trs:Password changed.}";
    } catch(ex) {
        passwordStatus.textContent = `{trs:Could not change the password:} ${ex.message}`;
    }
};

(async () => {
    user = await OSRS.getUser();
    if(user.uid == -1) {
        OSRS.redirectToLogin();
        return;
    }
    document.querySelector("#profile-username").textContent = user.username;
    document.querySelector("#profile-uid").textContent = user.uid;
    const sets = await OSRS.getSets();
    if(sets.length == 0) {
        const elem = document.createElement("li");
        elem.textContent = "{trs:No sets loaded. Go install some!}";
        setsList.appendChild(elem);
    }
    for(const set of sets) {
        appendSetEntry(set);
    }
})();
