Object.defineProperty(window, "explode", {
    value: function(parentNode, content){
        let standardText = "";
        let indexOfStart;
        while((indexOfStart = content.indexOf("{")) !== -1){
            standardText = content.substring(0, indexOfStart);
            content = content.substring(indexOfStart + 1);
            if(content.charAt(indexOfStart + 1) === '{'){
                standardText += '{';
                content = content.substring(1);
                parentNode.append(standardText);
                continue;
            }
            parentNode.append(standardText);
    
            //Split arguments and call module
            let indexOfEnd = content.indexOf("}");
            let exploded = content.substring(0, indexOfEnd);
            content = content.substring(indexOfEnd + 1);

            let splittingChar = '😳';
            for(let c of exploded){
                let i = c.charCodeAt(0);
                if(i > 32 && i < 65){
                    splittingChar = c;
                    break;
                }
            }
    
            let arguments = exploded.split(splittingChar);
            let base = arguments[0];
            let args = [];
            let kwargs = {};
    
            for(let i = 1; i<arguments.length; i++){
                if(arguments[i].includes("=")){
                    //KWARG
                    let [name, value] = arguments[i].split("=");
                    kwargs[name] = value;
                }else{
                    args.push(arguments[i]);
                }
            }
    
            if(Object.keys(window.explodeModules).includes(base)){
                parentNode = window.explodeModules[base](parentNode, args, kwargs) ?? parentNode;
            }else{
                console.error("[Explode]: Module %s doesn't exist for exploded %s!", base, exploded);
            }
        }
        parentNode.append(content);
    },
    writable: false
});

Object.defineProperty(window, "explodeModules", {
    value: {
        splitParent: function(parentNode){
            let newParent = document.createElement(parentNode.tagName);
            parentNode.insertAdjacentElement('afterend', newParent);
            return newParent;
        },
        span: function(parentNode, content, attrs){
            let span = document.createElement("span");
            span.innerText = content.join('');
            for(let key of Object.keys(attrs)){
                span.setAttribute(key, attrs[key]);
            }
            parentNode.appendChild(span);
        }
    },
    writable: true,
});
