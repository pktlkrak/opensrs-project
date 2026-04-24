window.explodeModules['tooltip'] = function(parentNode, [type, ...content]){
    window.explodeModules.span(parentNode, content, {
        class: `${type.toLowerCase()}-highlight`,
        title: '',
        rel: 'tooltip',
        "data-original-title": type,
    });
}

window.explodeModules['linebreak'] = (pN) => pN.appendChild(document.createElement("br"));
window.explodeModules['link'] = function(parentNode, [target, text]){
    let a = document.createElement("a");
    a.href = target;
    a.innerText = text;
    parentNode.appendChild(a);
}
