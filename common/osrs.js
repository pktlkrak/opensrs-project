(function(){
    const compileToUrlEncoded = data => {
        let output = "";
        for(let key of Object.keys(data)){
            const value = data[key];
            if(Array.isArray(value)){
                for(let item of value){
                    output += `${key}[]=${item}&`;
                }
            }else{
                output += `${key}=${item}&`;
            }
        }
        return output.substring(0, output.length - 1);
    };
    const get = (url, content) => new Promise((ok, fail) => fetch("http://localhost:9998" + url, content === undefined ? undefined : {method: "POST", body: content, headers: {"Content-Type": "application/x-www-form-urlencoded"}}).then(n => n.json()).then(ok).catch(fail));
    const post = url => fetch("http://localhost:9998" + url, {method: "POST"}).then(n => n.ok ? n.text() : n.text().then(t => Promise.reject(new Error(t))));
    const osrs = {
        getCurrentSetName: () => new URL(window.location.href).pathname.split("/")[1],
        getUserStatus: () => get(`/api/${osrs.getCurrentSetName()}/progress/phases`),
        getLessonsQueue: () => get(`/api/${osrs.getCurrentSetName()}/lesson/queue`),
        getReviewsQueue: () => get(`/api/${osrs.getCurrentSetName()}/review/queue`),
        getUser: () => get(`/api/user`),
        getSets: () => get(`/api/sets`),
        initSetFor: (uid, set) => post(`/management/initSetForUser/${set}/${uid}`),
        dropSetFor: (uid, set) => post(`/management/dropSetForUser/${set}/${uid}`),
        redirectToLogin: () => window.location.href = `/${(window.location.pathname.match(/^\/(pl|en)\//) || [null, "en"])[1]}/login.html?redir=${encodeURIComponent(window.location.href)}`,
        getLastSessionLessonsDone: () => get(`/api/${osrs.getCurrentSetName()}/lesson/lastSession`),
        getLastSessionReviewsDone: () => get(`/api/${osrs.getCurrentSetName()}/review/lastSession`),
        startNewReviewSession: () => get(`/api/${osrs.getCurrentSetName()}/review/start`),
        startNewLessonSession: () => get(`/api/${osrs.getCurrentSetName()}/lesson/start`),
        redirectToSetHome: () => window.location.href = `/${osrs.getCurrentSetName()}/index.html`,
        sendReviewProgress: (itemId, reviewables) => get(`/api/${osrs.getCurrentSetName()}/review/progress`, compileToUrlEncoded({[itemId]: reviewables})),
        getInfoById: (id) => get(`/api/${osrs.getCurrentSetName()}/item/by-id/${id}`),
        getInfoByFamily: (family, query) => get(`/api/${osrs.getCurrentSetName()}/item/by-family/${family}/${query}`),
        sendLessonProgress: (itemId) => get(`/api/${osrs.getCurrentSetName()}/lesson/completed`, compileToUrlEncoded({keys: [itemId]})),
        getLevelItems: (level) => get(`/api/${osrs.getCurrentSetName()}/item/by-level/${level}`)
    };

    Object.defineProperty(window, "OSRS", {
        value: osrs,
        writable: false
    });
})();