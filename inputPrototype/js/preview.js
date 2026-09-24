// js/preview.js

document.addEventListener('DOMContentLoaded', () => {
    const btnPreview = document.getElementById('btn-preview');
    const panel = document.getElementById('preview-panel');
    const btnClose = document.getElementById('btn-close-preview');
    const btnCopy = document.getElementById('btn-copy');
    const codeBlock = document.getElementById('json-preview-code');
    
    let currentJsonString = '';

    btnPreview.addEventListener('click', () => {
        if (!state.currentSchemaId) return;
        
        const data = state.getSchemaState(state.currentSchemaId);
        currentJsonString = JSON.stringify(data, null, 2);
        
        // Remove empty arrays/objects and nulls for a cleaner output if desired,
        // but straight stringify is usually fine for this requirement.
        
        codeBlock.textContent = currentJsonString;
        
        // Apply syntax highlighting
        delete codeBlock.dataset.highlighted;
        hljs.highlightElement(codeBlock);
        
        panel.classList.add('active');
    });

    btnClose.addEventListener('click', () => {
        panel.classList.remove('active');
    });

    // Close on background click
    panel.addEventListener('click', (e) => {
        if (e.target === panel) {
            panel.classList.remove('active');
        }
    });

    // Copy to clipboard
    btnCopy.addEventListener('click', async () => {
        try {
            await navigator.clipboard.writeText(currentJsonString);
            
            const originalText = btnCopy.innerHTML;
            btnCopy.innerHTML = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 6L9 17l-5-5"></path></svg> Copied!`;
            btnCopy.classList.add('success-flash');
            
            setTimeout(() => {
                btnCopy.innerHTML = originalText;
                btnCopy.classList.remove('success-flash');
            }, 2000);
        } catch (err) {
            console.error('Failed to copy text: ', err);
            alert('Failed to copy to clipboard.');
        }
    });
});
