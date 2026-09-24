// js/download.js

// IndexedDB wrapper for storing the directory handle per schema
const DB_NAME = 'SelfOS_DB';
const STORE_NAME = 'schema_settings'; // renamed store to make it schema specific

function openDB() {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open(DB_NAME, 2); // bumped version
        request.onupgradeneeded = (e) => {
            if (!request.result.objectStoreNames.contains(STORE_NAME)) {
                request.result.createObjectStore(STORE_NAME);
            }
        };
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    });
}

async function getStoredHandle(schemaId) {
    if (!schemaId) return null;
    try {
        const db = await openDB();
        return new Promise((resolve, reject) => {
            const tx = db.transaction(STORE_NAME, 'readonly');
            const store = tx.objectStore(STORE_NAME);
            const request = store.get(`dir_${schemaId}`);
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    } catch (e) {
        return null;
    }
}

async function storeHandle(schemaId, handle) {
    if (!schemaId) return;
    const db = await openDB();
    return new Promise((resolve, reject) => {
        const tx = db.transaction(STORE_NAME, 'readwrite');
        const store = tx.objectStore(STORE_NAME);
        const request = handle ? store.put(handle, `dir_${schemaId}`) : store.delete(`dir_${schemaId}`);
        request.onsuccess = () => resolve();
        request.onerror = () => reject(request.error);
    });
}

async function verifyPermission(fileHandle) {
    const options = { mode: 'readwrite' };
    if ((await fileHandle.queryPermission(options)) === 'granted') return true;
    if ((await fileHandle.requestPermission(options)) === 'granted') return true;
    return false;
}

document.addEventListener('DOMContentLoaded', async () => {
    const btnImportJson = document.getElementById('btn-import-json');
    const btnDownloadDefault = document.getElementById('btn-download-default');
    const btnDownloadConfigured = document.getElementById('btn-download-configured');
    const btnSelectFolder = document.getElementById('btn-select-folder');

    // Setup Folder UI on Load
    async function updateFolderUI() {
        if (!state.currentSchemaId) return;
        if (!btnSelectFolder) return;
        
        const handle = await getStoredHandle(state.currentSchemaId);
        if (handle) {
            btnSelectFolder.innerHTML = `Set Folder: <b>${handle.name}</b>`;
            btnSelectFolder.title = "Click to select a new folder or clear";
            btnDownloadConfigured.classList.remove('btn-secondary');
            btnDownloadConfigured.classList.add('btn-primary');
        } else {
            btnSelectFolder.innerHTML = `Set Folder: <b>None</b>`;
            btnSelectFolder.title = "Select save location directly to folder";
            // If no folder set, visually deemphasize the "Save to folder" button
            btnDownloadConfigured.classList.remove('btn-primary');
            btnDownloadConfigured.classList.add('btn-secondary');
        }
    }

    // Listen for schema changes from app.js to reload UI
    window.addEventListener('schemaChanged', async (e) => {
        await updateFolderUI();
    });

    if (btnSelectFolder) {
        btnSelectFolder.addEventListener('click', async () => {
            if (!state.currentSchemaId) return;
            const currentHandle = await getStoredHandle(state.currentSchemaId);
            
            // If API not supported, alert and exit.
            if (!('showDirectoryPicker' in window)) {
                alert("Your browser doesn't support the File System Access API. Standard downloads will be used.");
                return;
            }

            if (currentHandle) {
                // Let user decide whether to clear or replace the folder check
                if (confirm(`Current save folder is "${currentHandle.name}". Do you want to CLEAR it? \n\n(Click Cancel to choose a new folder instead)`)) {
                    await storeHandle(state.currentSchemaId, null);
                    await updateFolderUI();
                    return;
                }
            }
            
            try {
                // Request a new directory
                const handle = await window.showDirectoryPicker({ mode: 'readwrite' });
                await storeHandle(state.currentSchemaId, handle);
                await updateFolderUI();
            } catch (err) {
                if (err.name !== 'AbortError') console.error('Directory picker error:', err);
            }
        });
    }

    const formatFilenameDate = () => {
        const d = new Date();
        const day = d.getDate();
        const monthMatch = d.toLocaleString('en-US', { month: 'long' });
        
        let hours = d.getHours();
        const minutes = d.getMinutes().toString().padStart(2, '0');
        const ampm = hours >= 12 ? 'PM' : 'AM';
        
        hours = hours % 12;
        hours = hours ? hours : 12;
        
        return `${day} ${monthMatch} ${hours}-${minutes} ${ampm}`;
    };

    // --- IMPORT JSON BUTTON ---
    if (btnImportJson) {
        btnImportJson.addEventListener('click', async () => {
            if (!state.currentSchemaId) return;

            // Use File System Access API if available
            if ('showOpenFilePicker' in window) {
                try {
                    const pickerOptions = {
                        types: [{
                            description: 'JSON Files',
                            accept: { 'application/json': ['.json'] }
                        }],
                        multiple: false
                    };

                    const currentHandle = await getStoredHandle(state.currentSchemaId);
                    if (currentHandle) {
                        pickerOptions.startIn = currentHandle;
                    }

                    const [fileHandle] = await window.showOpenFilePicker(pickerOptions);
                    const file = await fileHandle.getFile();
                    const content = await file.text();
                    
                    try {
                        const jsonData = JSON.parse(content);
                        state.setSchemaState(state.currentSchemaId, jsonData);
                        state.importedFilename = file.name;
                        window.dispatchEvent(new CustomEvent('schemaHydrated'));
                        flashSuccess(btnImportJson, "Imported!");
                    } catch(e) {
                        alert("Error parsing JSON file: " + e.message);
                    }
                } catch (err) {
                    if (err.name !== 'AbortError') console.error('File picker error:', err);
                }
            } else {
                // Fallback to traditional file input
                const input = document.createElement('input');
                input.type = 'file';
                input.accept = 'application/json';
                input.onchange = e => {
                    const file = e.target.files[0];
                    if (!file) return;
                    const reader = new FileReader();
                    reader.onload = event => {
                        try {
                            const jsonData = JSON.parse(event.target.result);
                            state.setSchemaState(state.currentSchemaId, jsonData);
                            state.importedFilename = file.name;
                            window.dispatchEvent(new CustomEvent('schemaHydrated'));
                            flashSuccess(btnImportJson, "Imported!");
                        } catch(err) {
                            alert("Error parsing JSON file: " + err.message);
                        }
                    };
                    reader.readAsText(file);
                };
                input.click();
            }
        });
    }

    // --- STANDARD DOWNLOAD BUTTON ---
    btnDownloadDefault.addEventListener('click', async () => {
        if (!state.currentSchemaId) return;
        
        const data = state.getSchemaState(state.currentSchemaId);
        const jsonString = JSON.stringify(data, null, 2);
        const filenameStr = state.importedFilename || `${state.currentSchemaId} ${formatFilenameDate()}.json`;
        
        fallbackDownload(jsonString, filenameStr, btnDownloadDefault);
    });

    // --- SAVE TO CONFIGURED FOLDER BUTTON ---
    btnDownloadConfigured.addEventListener('click', async () => {
        if (!state.currentSchemaId) return;
        
        const data = state.getSchemaState(state.currentSchemaId);
        const jsonString = JSON.stringify(data, null, 2);
        const filenameStr = state.importedFilename || `${state.currentSchemaId} ${formatFilenameDate()}.json`;
        
        const dirHandle = await getStoredHandle(state.currentSchemaId);
        
        if (!dirHandle) {
            // No folder set for this schema, prompt user to set one
            alert("No configured folder for this schema! Please click 'Set Folder' first, or use the standard 'Download' button.");
            // Trigger the set folder click
            btnSelectFolder.click();
            return;
        }

        if (dirHandle && 'showDirectoryPicker' in window) {
            try {
                // Ask the user to re-grant permission if they've restarted the browser
                if (await verifyPermission(dirHandle)) {
                    const fileHandle = await dirHandle.getFileHandle(filenameStr, { create: true });
                    const writable = await fileHandle.createWritable();
                    await writable.write(jsonString);
                    await writable.close();
                    flashSuccess(btnDownloadConfigured);
                    return;
                }
            } catch (err) {
                console.error("Error writing to directory handle:", err);
                alert("Could not write to the saved folder. Check permissions or select a new folder.\nFalling back to normal download.");
                await storeHandle(state.currentSchemaId, null);
                await updateFolderUI();
            }
        }
        
        // Fallback to traditional download if permission denied
        fallbackDownload(jsonString, filenameStr, btnDownloadConfigured);
    });
    
    function fallbackDownload(content, filename, btn) {
        const blob = new Blob([content], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        
        setTimeout(() => {
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        }, 100);
        
        flashSuccess(btn);
    }
    
    function flashSuccess(btn, message = "Saved!") {
        const originalText = btn.innerHTML;
        // Strip the text content from original html loosely
        if(originalText.includes('Downloaded') || originalText.includes('Saved') || originalText.includes('Imported')) return;
        
        btn.innerHTML = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 6L9 17l-5-5"></path></svg> ${message}`;
        btn.classList.add('success-flash');
        
        setTimeout(() => {
            btn.innerHTML = originalText;
            btn.classList.remove('success-flash');
        }, 2000);
    }
});
