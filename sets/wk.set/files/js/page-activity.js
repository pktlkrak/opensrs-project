(async () => {

    const inform = alert; //TODO: this
    const shuffle = array => array.sort(() => Math.random() - 0.5);
    const capitalizeFirstLetter = e => e.substring(0, 1).toUpperCase() + e.substring(1);

    function updateReviewStats() {
        document.querySelector("#review-available-count").innerText = window.review.statHelpers.itemCount - window.review.statHelpers.sent;
        const rate = parseInt((100 * window.review.statHelpers.correct) / (window.review.statHelpers.correct + window.review.statHelpers.incorrect));
        document.querySelector("#review-correct-rate").innerText = isNaN(rate) ? 100 : rate;
        document.querySelector("#review-completed-count").innerText = window.review.statHelpers.sent;
    }

    function loadReviewItemFromList() {
        const placeholders = {
            'reading': '答え',
        }

        const langs = {
            'reading': 'ja',
            'meaning': 'en'
        }
        const infoButton = document.querySelector("#option-item-info");
        const infoField = document.querySelector("#item-info");
        const infoParent = document.querySelector("#information");

        infoButton.classList.add("disabled");
        infoButton.onclick = null;
        infoField.innerHTML = "";
        infoParent.style.display = "none";

        const itemToDisplay = window.review.itemList.splice(0, 1)[0];
        const character = document.querySelector("#review-character");
        character.className = itemToDisplay.family;
        character.innerHTML = "";
        if(itemToDisplay.query.includes("images/")){
            const img = document.createElement("img");
            img.src = itemToDisplay.query;
            img.alt = itemToDisplay.query;
            character.appendChild(img);
        }else{
            character.innerText = itemToDisplay.query;
        }
window.review.currentId = itemToDisplay.id;

        const questionType = document.querySelector("#question-type");
        questionType.innerHTML =
            `<h1>${capitalizeFirstLetter(itemToDisplay.family)} <strong>${itemToDisplay.reviewable}</strong></h1>`;
        questionType.className = ['reading', 'meaning'].includes(itemToDisplay.reviewable) ?
            itemToDisplay.reviewable : "meaning";

        const userResponse = document.querySelector("#user-response");
        userResponse.lang = langs[itemToDisplay.reviewable] ?? 'en';
        userResponse.placeholder = placeholders[itemToDisplay.reviewable] ?? "Your Response";

        const fieldSet = document.querySelector("#answer-form > form > fieldset");

        const useIME = itemToDisplay.reviewable === "reading";

        document.querySelector("#answer-form > form").onsubmit = () => {
            if (fieldSet.className != "") {
                if (window.review.itemList.length == 0) window.review.afterReviews();
                else loadReviewItemFromList();
                userResponse.value = "";
                fieldSet.className = "";
            } else {
                let value = userResponse.value;

                if (useIME) {
                    value = wanakana.toRomaji(value).replace(/\-/g, 'ー').replace(/'/g, '');
                }

                value = value.trim()

                if (itemToDisplay.coerce.hasOwnProperty(value)) {
                    inform(itemToDisplay.coerce[value]);
                    userResponse.value = "";
                    return false;
                }

                if (useIME) {
                    wanakana.unbind(userResponse);
                }

                const correct = itemToDisplay.answers.includes(value.toLowerCase());

                if (!window.review.itemUserResponses.hasOwnProperty(itemToDisplay.id)) {
                    window.review.itemUserResponses[itemToDisplay.id] = Array(itemToDisplay.allReviewables)
                        .fill(-1);
                }

                if(window.review.itemUserResponses[itemToDisplay.id][itemToDisplay.thisReviewableIndex] === -1){
                    window.review.itemUserResponses[itemToDisplay.id][itemToDisplay.thisReviewableIndex] = 0;
                }
                if (correct) {
                    //Check if every reviewable of this item has been reviewed
                    window.review.statHelpers.correct++;
                    if (!window.review.itemUserResponses[itemToDisplay.id].filter(n => n == -1).length) {
                        //Every aspect checked, send to the server
                        window.review.endpoint(itemToDisplay);
                        delete window.review.itemUserResponses[itemToDisplay.id];
                        window.review.statHelpers.sent++;
                    }
                    fieldSet.className = "correct";
                } else {
                    window.review.statHelpers.incorrect++;
                    window.review.itemList.push(itemToDisplay); //Redo it
                    shuffle(window.review.itemList);
                    fieldSet.className = "incorrect";
                    window.review.itemUserResponses[itemToDisplay.id][itemToDisplay.thisReviewableIndex] += 1;
                }

                infoButton.classList.remove("disabled");
                infoButton.onclick = () => {
                    const aifr = document.createElement("iframe");
                    aifr.src = `/${OSRS.getCurrentSetName()}/${itemToDisplay.family}/${itemToDisplay.query}?raw`;
                    infoField.appendChild(aifr);
                    infoParent.style.display = "block";
                    infoButton.onclick = null;
                };
                updateReviewStats();
            }

            return false;
        };

        if (useIME) {
            wanakana.bind(userResponse);
        }
    }

    function doReview(items, endpoint, then) {
        document.querySelector("#lessons").style.display = "none";
        document.querySelector("#reviews").style.display = "block";
        //Every item follows a template
        /*
            {
                id: numeric osrs id,
                family: family of elements this belongs to
                query: the thing it shows,
                reviewable: type of check, like reading or meaning,
                allReviewables: amount of all reviewables,
                thisReviewableIndex: index of this reviewable
                answers: [correct answers],
                coerce: {incorrect value => message for user}
            }
        */
        shuffle(items);
        window.review = {
                itemList: items,
                itemUserResponses: {},
                statHelpers: {
                    correct: 0,
                    incorrect: 0,
                    sent: 0,
                    itemCount: new Set(items.map(n => n.id)).size
                },
                afterReviews: then,
                endpoint: endpoint
        };
        updateReviewStats();
        loadReviewItemFromList();
    }

    const user = await OSRS.getUser();

    setInterval(() => OSRS.getUser(), 60000); // Eh... Session logout prevention. Hopefully temporary...

    if (user.uid == -1) {
        OSRS.redirectToLogin();
        return;
    }
    const lessonMode = window.location.href.includes("lesson");
    if (lessonMode) {
        await OSRS.startNewLessonSession();
        doLesson();
    } else {
        //Review
        await OSRS.startNewReviewSession();
        const reviewQueue = await OSRS.getReviewsQueue();
        const items = reviewItemsFromQueue(reviewQueue.queue);

        doReview(items,
            async (itemToDisplay) => {
                let e = await OSRS.sendReviewProgress(itemToDisplay.id, window.review.itemUserResponses[itemToDisplay.id]);
                if(e.errorCode) window.location.reload();
            },
            () => window.location.href = `/${OSRS.getCurrentSetName()}/review`);
    }

    function reviewItemsFromQueue(queue){
        let items = [];
        for (let item of queue) {
            let i = 0;
            for (let reviewable of Object.keys(item.reviewable)) {
                items.push({
                    id: item.id,
                    family: item.family,
                    query: item.query,
                    reviewable: reviewable,
                    allReviewables: Object.keys(item.reviewable).length,
                    thisReviewableIndex: i,
                    answers: item.reviewable[reviewable].map(n => n.toLowerCase()),
                    coerce: item.coerce
                });
                i += 1;
            }
        }
        return items;
    }

    //**********************REVIEW CODE DONE, LESSONS BELOW**************************/

    function updateLessonStats() {
        document.querySelector("#lesson-completed-count").innerText = window.lesson.statHelpers.completed;
        document.querySelector("#lesson-vocabulary-count span").innerText = window.lesson.statHelpers.vocabRemain;
        document.querySelector("#lesson-kanji-count span").innerText = window.lesson.statHelpers.kanjiRemain;
        document.querySelector("#lesson-radical-count span").innerText = window.lesson.statHelpers.radicalsRemain;

        document.querySelectorAll("#lesson-stats span").forEach(n => n.className = window.lesson.itemList[window.lesson.currentItem]?.family ?? "");
    }

    async function doLesson() {
        document.querySelector("#lessons").style.display = "block";
        document.querySelector("#reviews").style.display = "none";

        const lessonQueueAll = (await OSRS.getLessonsQueue()).queue;
        const lessonQueue = lessonQueueAll.slice(0, 5);

        function getMeaning(item){
            return capitalizeFirstLetter(item.family === "radical" ? item.reviewable['name'][0] : item.reviewable['meaning'][0]);
        }

        function createTile(family, query, title){
            const li = document.createElement("li");
            if(query.includes("images/")){
                query = `<img src="${query}" alt="${query}">`;
            }
            li.className = "pure-u-1";
            li.innerHTML = `<a href="/${OSRS.getCurrentSetName()}/${family}/${query}" target="_blank" rel="noreferrer noopener"><span
                    class="${family}" lang="ja">${query}</span>
                    <div>${title}</div>
                </a>`;
            return li;
        }

        function jumpToLessonItemPage(page){
            page = parseInt(page);
            const pageE = document.querySelector("#supplement-voc-breakdown");
            document.querySelector(`#supplement-nav li[data-index='${window.lesson.currentPage}']`)?.classList.remove("active");
            document.querySelector(`#supplement-nav li[data-index='${page}']`).classList.add("active");
            window.lesson.currentPage = page;
            pageE.innerHTML = "";
            pageE.appendChild(window.lesson.itemList[window.lesson.currentItem].pages[page].page);
        }

        function jumpToLessonItem(item){
            if(item === "quiz"){
                //Switch modes
                doReview(reviewItemsFromQueue(window.lesson.rawToPassToReview),
                    (item) => {
                        OSRS.sendLessonProgress(item.id);
                    },
                    () => {
                        //Are we done?
                        if(window.lesson.statHelpers.vocabRemain +
                            window.lesson.statHelpers.kanjiRemain +
                            window.lesson.statHelpers.radicalsRemain === 0){
                                //Yes
                                window.location.href = "lesson";
                            }else{
                                //Nope
                                doLesson();
                            }
                    });
            }else{
                item = parseInt(item);
                const toc = document.querySelector("#supplement-nav ul");
                toc.innerHTML = "";
                window.lesson.currentItem = item;

                let i = 0;
                for(const page of window.lesson.itemList[item].pages){
                    const link = document.createElement("li");
                    link.className = i === 0 ? "active" : "";
                    link.setAttribute("data-index", i++);
                    link.innerText = page.name;
                    link.onclick = (e) => jumpToLessonItemPage(e.target.getAttribute("data-index"));
                    toc.appendChild(link);
                }
                const char = window.lesson.itemList[item].character;
                if(char.includes("images/")){
                    const img = document.createElement("img");
                    img.src = char;
                    document.querySelector("#lesson-character").innerHTML = "";
                    document.querySelector("#lesson-character").appendChild(img);
                }else{
                    document.querySelector("#lesson-character").innerText = window.lesson.itemList[item].character;
                }
                document.querySelector("#meaning").innerText = window.lesson.itemList[item].meaning;
                document.querySelector("#main-info").className = window.lesson.itemList[item].family;
                jumpToLessonItemPage(0);
            }
        }

        function numberToWord(n){
            return ["zero", "one", "two", "three", "four", "five"][n];
        }

        function createPages(item){
            if(item.family === "radical"){
                const namePage = document.createElement("div");
                namePage.innerHTML = `<div class="pure-u-1 col1"><h2>Name Mnemonic</h2><p></p></div>`;
                namePage.class = "pure-g-r";
                explode(namePage.querySelector("p"), item.clientData.mnemonic);

                const examplesPage = document.createElement("div");
                examplesPage.innerHTML = `<div class="pure-u-1 col1"><h2>Kanji Examples</h2><p>Here's a glympse of some of the kanji you will be learning that utilize <strong>${getMeaning(item)}</strong></p><ul class="pure-g-r"></ul></div>`;
                examplesPage.class = "pure-g-r";
                const examplesList = examplesPage.querySelector("ul");
                item.itemsDependent.splice(0, 3).forEach(n => examplesList.appendChild(createTile(n.family, n.query, getMeaning(n))))
                return [
                    {
                        name: "Name",
                        page: namePage
                    }, {
                        name: "Kanji Examples",
                        page: examplesPage
                    }
                ];
            }else if(item.family === "kanji"){
                const radicalPage = document.createElement("div");
                radicalPage.className = "pure-g-r";
                radicalPage.innerHTML = `<div class="pure-u-1 col1"><h2>Radical Composition</h2><p>The kanji is composed of <span class="supplement-kan-count">${numberToWord(item.dependencies.length)} radical(s)</span>. Can you see where the radical fits<span class="supplement-kan-question"></span> in the kanji?</p><ul class="pure-g-r"></ul></div>`;
                const radicalsList = radicalPage.querySelector("ul");
                item.dependencies.forEach(n => radicalsList.appendChild(createTile(n.family, n.query, getMeaning(n))));

                const meaningPage = document.createElement("div");
                meaningPage.className = "pure-g-r";
                meaningPage.innerHTML = `
                <div class="pure-u-1-4 col1">
                    <h2>Synonyms</h2>
                    <div id="synonym">(None)</div>
                </div>
                <div class="pure-u-3-4 col2">
                    <h2>Meaning Mnemonic</h2>
                    <div id="supplement-kan-meaning-mne"></div>
                    <blockquote>
                        <h3><i class="icon-question-sign"></i> HINT</h3>
                        <div id="supplement-kan-meaning-hnt"></div>
                    </blockquote>
                </div>`;
                meaningPage.querySelector("#synonym").innerText = item.clientData.meanings["Alternative"] ?? "(None)";
                explode(meaningPage.querySelector("#supplement-kan-meaning-mne"), item.clientData.mnemonic);
                explode(meaningPage.querySelector("#supplement-kan-meaning-hnt"), item.clientData.hints);

                const readingsData = item.clientData.kanjiReadings.readings.filter(n => n.primary)[0];
                const readingsPage = document.createElement("div");
                readingsPage.className = "pure-g-r";
                readingsPage.innerHTML = `
                <div class="pure-u-1-4 col1">
                    <h2>Readings (${readingsData.name})</h2>
                    <div><span lang="ja">${readingsData.reading}</span></div>
                </div>
                <div class="pure-u-3-4 col2">
                    <h2>Reading Mnemonic</h2>
                    <div id="supplement-kan-reading-mne"></div>
                    <blockquote>
                        <h3><i class="icon-question-sign"></i> HINT</h3>
                        <div id="supplement-kan-reading-hnt"></div>
                    </blockquote>
                </div>`;
                explode(readingsPage.querySelector("#supplement-kan-reading-mne"), item.clientData.kanjiReadings.mnemonic);
                explode(readingsPage.querySelector("#supplement-kan-reading-hnt"), item.clientData.kanjiReadings.hints);

                const examplesPage = document.createElement("div");
                examplesPage.className = "pure-g-r";
                examplesPage.innerHTML = `
                <div class="pure-u-1 col1">
                    <h2>Vocabulary Examples</h2>
                    <p>Here is a glimpse of some of the vocabulary you will be learning in the future that utilize the kanji:
                    </p>
                    <ul class="pure-g-r">

                    </ul>
                </div>`;
                const list = examplesPage.querySelector("ul");
                item.itemsDependent.slice(0, 3).forEach(n => list.appendChild(createTile(n.family, n.query, getMeaning(n))));

                return [
                    {
                        name: "Radicals",
                        page: radicalPage
                    }, {
                        name: "Meaning",
                        page: meaningPage
                    }, {
                        name: "Readings",
                        page: readingsPage
                    }, {
                        name: "Examples",
                        page: examplesPage
                    }
                ]
            }else if(item.family === "vocabulary"){
                const kanjiPage = document.createElement("div");
                kanjiPage.className = "pure-g-r";
                kanjiPage.innerHTML = `
                <div class="pure-u-1 col1">
                    <h2>Kanji Composition</h2>
                    <p>The vocabulary is composed of <span>${numberToWord(item.dependencies.length)} kanji:</span></p>
                    <ul class="pure-g-r"></ul>
                    <p>Does the combination of the kanji meanings somewhat relate
                        to the vocabulary meaning?</p>
                    <p>Based on the kanji composition, can you guess the reading of
                        the vocabulary?</p>
                </div>`;
                const kanjiList = kanjiPage.querySelector("ul");
                item.dependencies.forEach(n => kanjiList.appendChild(createTile(n.family, n.query, getMeaning(n))));

                const meaningsPage = document.createElement("div");
                meaningsPage.className = "pure-g-r";
                meaningsPage.innerHTML = `
                <div class="pure-u-1-4 col1">
                    <h2>Synonyms</h2>
                    <div id="supplement-voc-synonyms"></div>
                    <h2>Word Type</h2>
                    <div id="supplement-voc-meaning-part-of-speech"></div>
                </div>
                <div class="pure-u-3-4 col2">
                    <h2>Meaning Explanation</h2>
                    <div id="supplement-voc-meaning-exp"></div>
                </div>`;
                explode(meaningsPage.querySelector("#supplement-voc-meaning-exp"), item.clientData.mnemonic);
                meaningsPage.querySelector("#supplement-voc-synonyms").innerText = item.clientData.meanings["Alternative"] ?? "(None)";
                meaningsPage.querySelector("#supplement-voc-meaning-part-of-speech").innerText = item.clientData.meanings["Word Type"] ?? "(None)";

                const readingPage = document.createElement('div');
                readingPage.className = "pure-g-r";
                readingPage.innerHTML = `
                <div class="pure-u-1-4 col1">
                    <h2>Vocab Reading</h2>
                    <div class="readings-component">
                    </div>
                </div>
                <div class="pure-u-3-4 col2">
                    <h2>Reading Explanation</h2>
                    <div id="supplement-voc-reading-exp"></div>
                </div>`;
                explode(readingPage.querySelector("#supplement-voc-reading-exp"), item.clientData.vocabReadings.mnemonic);
                const root = readingPage.querySelector(".readings-component");
                item.clientData.vocabReadings.readings.forEach(e => {
                    const pgr = document.createElement("div");
                    pgr.className = "pronunciation-group";
                    const p = document.createElement("p");
                    p.className = "pronunciation-variant";
                    p.innerText = e.reading;
                    pgr.appendChild(p);
                    root.appendChild(pgr);
                });

                const contextPage = document.createElement("div");
                contextPage.className = "pure-g-r";
                contextPage.innerHTML = `
                <div class="pure-u-1 col1">
                    <h2>Context Sentences</h2>
                </div>`;
                const contextDiv = contextPage.querySelector("div");
                item.clientData.contextSentences.forEach(n => {
                    const ja = document.createElement("p");
                    ja.lang = "ja";
                    ja.innerText = n.ja;
                    const en = document.createElement("p");
                    en.className = "english-sentence";
                    en.innerText = n.en;
                    const e = document.createElement("div");
                    e.className = "context-sentence-group";
                    e.appendChild(ja);
                    e.appendChild(en);
                    contextDiv.appendChild(e);
                })

                return [
                    {
                        name: "Kanji Composition",
                        page: kanjiPage
                    }, {
                        name: "Meaning",
                        page: meaningsPage
                    }, {
                        name: "Reading",
                        page: readingPage
                    }, {
                        name: "Context",
                        page: contextPage
                    }
                ]
            }
        }
        window.lesson = {
            rawToPassToReview: lessonQueue,
            itemList: lessonQueue.map(item => ({
                character: item.query,
                meaning: getMeaning(item),
                pages: createPages(item),
                family: item.family
            })),
            statHelpers: {
                completed: window.lesson?.statHelpers?.completed ?? 0, //Copy value from previous batch
                vocabRemain: lessonQueueAll.filter(n => n.family === "vocabulary").length,
                kanjiRemain: lessonQueueAll.filter(n => n.family === "kanji").length,
                radicalsRemain: lessonQueueAll.filter(n => n.family === "radical").length,
            },
            currentItem: 0,
            currentPage: 0,
            doneItems: []
        };
        updateLessonStats();
        const batch = document.querySelector("#batch-items > ul");
        batch.innerHTML = "";
        let i = 0;
        const onclickForBatch = (e) => {
            jumpToLessonItem(e.currentTarget.getAttribute("data-index"));
        }
        for(const item of window.lesson.itemList){
            const elem = document.createElement("li");
            elem.setAttribute("data-index", i++);
            elem.onclick = onclickForBatch;

            const span = document.createElement("span");
            span.className = item.family;
            span.lang = "ja";
            if(item.character.includes("images/")){
                const img = document.createElement("img");
                img.src = item.character;
                span.appendChild(img);
            }else{
                span.innerText = item.character;
            }
            elem.appendChild(span);
            batch.appendChild(elem);
        }
        const elem = document.createElement("li");
        elem.onclick = onclickForBatch;
        elem.setAttribute("data-index", "quiz");
        const span = document.createElement("span");
        span.innerHTML = 'Quiz<i class="icon-arrow-right"></i>';
        elem.appendChild(span);
        batch.appendChild(elem);

        document.querySelector("#prev-btn").onclick = () => {
            let page = window.lesson.currentPage;
            let item = window.lesson.currentItem;
            if(item === 0 && page == 0){
                item = window.lesson.itemList.length;
            }
            if(page === 0){
                item--;
                page = window.lesson.itemList[item].pages.length;
            }
            page--;
            jumpToLessonItem(item);
            jumpToLessonItemPage(page);
            updateLessonStats();
        };

        document.querySelector("#next-btn").onclick = () => {
            let page = window.lesson.currentPage;
            let item = window.lesson.currentItem;
            let itemC = window.lesson.itemList.length;
            let pageC = window.lesson.itemList[item].pages.length;
            if(page === pageC - 1){
                //Last page - next item
                page = -1;
                if(!window.lesson.doneItems.includes(item)){
                    window.lesson.doneItems.push(item);
                    document.querySelector(`#batch-items li[data-index='${item}']`).className = "read";
                    window.lesson.statHelpers.completed += 1;
                    window.lesson.statHelpers[{
                        radical: "radicalsRemain",
                        kanji: "kanjiRemain",
                        vocabulary: "vocabRemain"
                    }[window.lesson.itemList[item].family]]--;
                }
                window.lesson.currentItem = item+1;
                updateLessonStats();
                item++;
            }
            if(item === itemC){
                //Last page of last item - go to quiz
                if(window.lesson.doneItems.length != itemC){
                    jumpToLessonItem(0);
                    jumpToLessonItemPage(0);
                    return;
                }
                jumpToLessonItem("quiz");
                return;
            }

            page++;
            jumpToLessonItem(item);
            jumpToLessonItemPage(page);
        };

        jumpToLessonItem(0);
    }

})();
