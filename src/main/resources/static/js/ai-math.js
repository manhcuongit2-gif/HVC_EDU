(() => {
  const existingMath = /\$\$[\s\S]*?\$\$|\\\[[\s\S]*?\\\]|\\\([\s\S]*?\\\)|(?<!\\)\$(?!\$)[^\n$]*?(?<!\\)\$(?!\$)/g;
  const code = /```[\s\S]*?```|`[^`\n]*`/g;
  const latexCommand = /\\[A-Za-z]+/;

  const normalize = (value) => {
    let text = String(value || '');
    const preserved = [];
    const protect = (pattern) => {
      text = text.replace(pattern, part => {
        const token = `HCMATHKEEP${preserved.length}TOKEN`;
        preserved.push([token, part]);
        return token;
      });
    };

    protect(code);
    protect(existingMath);

    const output = [];
    for (let index = 0; index < text.length; index++) {
      if (text[index] !== '(') {
        output.push(text[index]);
        continue;
      }

      let depth = 1;
      let end = index + 1;
      for (; end < text.length && depth > 0 && end - index <= 512; end++) {
        if (text[end] === '\n' || text[end] === '\r') break;
        if (text[end] === '(') depth++;
        else if (text[end] === ')') depth--;
      }

      if (depth === 0) {
        const expression = text.slice(index + 1, end - 1);
        if (latexCommand.test(expression)) {
          output.push(`\\(${expression}\\)`);
          index = end - 1;
          continue;
        }
      }
      output.push(text[index]);
    }

    text = output.join('');
    for (const [token, part] of preserved) text = text.split(token).join(part);
    return text;
  };

  let queue = Promise.resolve();
  const typeset = (node) => {
    if (!node || !window.MathJax?.typesetPromise) return queue;
    queue = queue.catch(() => {}).then(async () => {
      if (!node.isConnected || !window.MathJax?.typesetPromise) return;
      window.MathJax.typesetClear?.([node]);
      await window.MathJax.typesetPromise([node]);
    }).catch(() => {});
    return queue;
  };

  window.HungCuongAIMath = { normalize, typeset };
})();
