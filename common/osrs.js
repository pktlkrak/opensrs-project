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
    const req = (url, content) => new Promise((ok, fail) => fetch(url, content === undefined ? undefined : {method: "POST", body: content, headers: {"Content-Type": "application/x-www-form-urlencoded"}}).then(n => n.json()).then(ok).catch(fail));
    const osrs = {
        getCurrentSetName: () => new URL(window.location.href).pathname.split("/")[1],
        getUserStatus: () => req(`/api/${osrs.getCurrentSetName()}/progress/phases`),
        getLessonsQueue: () => req(`/api/${osrs.getCurrentSetName()}/lesson/queue`),
        getReviewsQueue: () => req(`/api/${osrs.getCurrentSetName()}/review/queue`),
        getUser: () => req(`/api/user`),
        redirectToLogin: () => window.location.href = `/login.html?redir=${encodeURIComponent(window.location.href)}`,
        getLastSessionLessonsDone: () => req(`/api/${osrs.getCurrentSetName()}/lesson/lastSession`),
        getLastSessionReviewsDone: () => req(`/api/${osrs.getCurrentSetName()}/review/lastSession`),
        startNewReviewSession: () => req(`/api/${osrs.getCurrentSetName()}/review/start`),
        startNewLessonSession: () => req(`/api/${osrs.getCurrentSetName()}/lesson/start`),
        redirectToSetHome: () => window.location.href = `/${osrs.getCurrentSetName()}/index.html`,
        sendReviewProgress: (itemId, reviewables) => req(`/api/${osrs.getCurrentSetName()}/review/progress`, compileToUrlEncoded({[itemId]: reviewables})),
        getInfoById: (id) => req(`/api/${osrs.getCurrentSetName()}/item/by-id/${id}`),
        getInfoByFamily: (family, query) => req(`/api/${osrs.getCurrentSetName()}/item/by-family/${family}/${query}`),
        sendLessonProgress: (itemId) => req(`/api/${osrs.getCurrentSetName()}/lesson/completed`, compileToUrlEncoded({keys: [itemId]})),
        getLevelItems: (level) => req(`/api/${osrs.getCurrentSetName()}/item/by-level/${level}`)
    };

    Object.defineProperty(window, "OSRS", {
        value: osrs,
        writable: false
    });
})();