$(document).ready(function () {
    const fillIn = (n, v) => document.querySelectorAll(`[data-insertValue='${n}']`).forEach(n => n
    .innerText = v);
    (async () => {
    const userInfo = await OSRS.getUser();
    if (userInfo.uid == -1) {
        OSRS.redirectToLogin();
        return;
    }
    fillIn("username", userInfo.username);
    const currentStatus = await OSRS.getUserStatus();
    const currentLesson = await OSRS.getLessonsQueue();
    const currentReview = await OSRS.getReviewsQueue();
    const currentItems = await OSRS.getLevelItems(currentStatus.level);

    let srsCounts = {
        "apprentice": {
        "radicals": 0,
        "kanji": 0,
        "vocabulary": 0,
        "total": 0
        },
        "guru": {
        "radicals": 0,
        "kanji": 0,
        "vocabulary": 0,
        "total": 0
        },
        "master": {
        "radicals": 0,
        "kanji": 0,
        "vocabulary": 0,
        "total": 0
        },
        "enlighten": {
        "radicals": 0,
        "kanji": 0,
        "vocabulary": 0,
        "total": 0
        },
        "burned": {
        "radicals": 0,
        "kanji": 0,
        "vocabulary": 0,
        "total": 0
        }
    }

    fillIn("level", currentStatus.level);


    for (let familyName in currentStatus.counts) {
        for (let id in currentStatus.counts[familyName]) {
        let subObject = srsCounts[Object.keys(srsCounts)[currentStatus.counts[familyName][id] - 1]];
        subObject.total += 1;
        subObject[familyName == "radical" ? "radicals" : familyName] += 1;
        }
    }

    function createProgressEntry(n) {
        const root = document.createElement("div");
        root.className = "progress-entry relative rounded-tr rounded-tl";
        const link = document.createElement("a");
        link.href = `/${OSRS.getCurrentSetName()}/${n.family}/${n.query}`;
        link.lang = "ja";
        link.className = `${n.family}-icon` + (n.rawStage === 0 && currentLesson.queue.filter(m => m.id === n.id).length === 0 ? ` ${n.family}-icon--locked` : ``);
        if (n.query.includes("images/")) {
        const img = document.createElement("img");
        img.src = n.query;
        img.alt = n.query;
        link.appendChild(img);
        } else {
        const s = document.createElement("span");
        s.innerText = n.query;
        link.appendChild(s);
        }
        const stage = Math.min(5, n.rawStage);
        const progressBar = document.createElement("div");
        progressBar.className = "progress-entry__pips w-full flex flex-row";
        for (let i = 0; i < 5; i++) {
        let className = "h-1 w-1/5 mr-px ";
        className += stage > i ? "bg-green " : "bg-gray-300 ";
        if (i === 0) {
            className += "rounded-l";
        } else if (i === 4) {
            className += "rounded-r"
        }
        const elem = document.createElement("div");
        elem.className = className;
        progressBar.appendChild(elem);
        }
        root.appendChild(link);
        root.appendChild(progressBar);
        return root;
    }

    const radicalsProgress = document.querySelector("#radicalsList");
    const kanjiProgress = document.querySelector("#kanjiList");
    currentItems.filter(n => n.family === "radical").forEach(n => radicalsProgress.appendChild(
        createProgressEntry(n)));

    const kanjis = currentItems.filter(n => n.family === "kanji");
    kanjis.forEach(n => kanjiProgress.appendChild(createProgressEntry(n)));
    const kanjisPassed = kanjis.filter(n => n.rawStage >= 5).length;

    fillIn("passedKanji", kanjisPassed);
    fillIn("allKanji", kanjis.length);

    const progress = (100.0 * kanjisPassed) / kanjis.length;

    if (kanjisPassed === 0) {
        document.querySelector("#mainKanjiProgress").remove();
    } else {
        const progressBar = document.querySelector("#mainKanjiProgress");
        progressBar.style.width = `${progress}%`;
        const label = document.querySelector("#mainKanjiProgressLabel");
        //if (progress > 20) {
        label.style.maxWidth = "calc(100% - 54px)";
        label.style.textAlign = "right";
        label.style.right = `calc(${100-progress}% + 36px)`;
        label.style.paddingLeft = "0";
        label.style.display = "";
        label.parentElement.classList.add("text-white");
        //}
    }



    // Fills in SRS counts
    Counts.fillInSrsCount(srsCounts);

    //Lessons / Reviews amount
    const minimumThresh = [500, 250, 100, 50, 25, 1, 0];
    const getMin = e => {
        let i = -1;
        while (e < minimumThresh[++i]);
        return minimumThresh[i];
    }

    const lessonsAmount = Object.keys(currentLesson.count).map(n => currentLesson.count[n]).reduce((a, b) =>
        a + b, 0);
    document.querySelectorAll("[data-insertValue='lessonsAmount']").forEach(n => n.innerText =
        lessonsAmount);
    document.querySelectorAll(".lessons-and-reviews__lessons-button").forEach(n => n.classList.add(
        "lessons-and-reviews__lessons-button--" + getMin(lessonsAmount)));

    const reviewsAmount = Object.keys(currentReview.count).map(n => currentReview.count[n]).reduce((a, b) =>
        a + b, 0);
    document.querySelectorAll("[data-insertValue='reviewsAmount']").forEach(n => n.innerText =
        reviewsAmount);
    document.querySelectorAll(".lessons-and-reviews__reviews-button").forEach(n => n.classList.add(
        "lessons-and-reviews__reviews-button--" + getMin(reviewsAmount)));


    })();


    // Replace dates with fuzzy dates
    DateTime.activateFuzzyDate();
    DateTime.activateRelativeDate();

    // SRS popover
    InfoTip.popoverSrs();

    // Message Status
    //Dashboard.messageStatus();
});
