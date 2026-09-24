// js/formBuilder.js

class FormBuilder {
    constructor(stateManager) {
        this.state = stateManager;
        this.container = document.getElementById('form-container');
        this.setupComputedFieldListeners();
    }

    setupComputedFieldListeners() {
        this.state.subscribe(() => {
            if (!this.state.currentSchemaId) return;
            const schema = SCHEMAS[this.state.currentSchemaId];
            this.updateComputedFields(schema.fields, "");

            const id = this.state.currentSchemaId;

            // Duration auto-calc for session-based schemas
            const durationSchemas = ['gaming_session', 'devotion', 'meditation', 'sexual_session', 'work_session'];
            if (durationSchemas.includes(id)) {
                this.runDurationCalculations(id, 'timestamp_start', 'timestamp_end', 'duration_minutes');
            }
            if (id === 'pain_log') {
                this.runDurationCalculations(id, 'pain_start', 'pain_end', 'duration_minutes');
            }

            if (id === 'sleep') {
                this.runSleepCalculations();
            }
            if (id === 'screentime') {
                this.runScreentimeCalculations();
            }
            if (id === 'work_session') {
                this.runWorkSessionCalculations();
            }
        });
    }

    formatLabel(key) {
        return key.replace(/_/g, ' ')
                  .replace(/\b\w/g, l => l.toUpperCase());
    }

    buildForm(schemaId) {
        this.container.innerHTML = '';
        const schema = SCHEMAS[schemaId];
        if (!schema) return;

        const title = document.createElement('h2');
        title.className = 'form-title';
        title.textContent = schema.label;
        this.container.appendChild(title);

        const form = document.createElement('form');
        form.id = `form-${schemaId}`;
        form.className = 'schema-form';
        form.onsubmit = (e) => e.preventDefault();
        
        // Single level grid by default
        const grid = document.createElement('div');
        grid.className = 'form-grid';
        
        for (const [key, fieldDef] of Object.entries(schema.fields)) {
            grid.appendChild(this.buildField(key, fieldDef, key));
        }
        
        form.appendChild(grid);
        this.container.appendChild(form);
        
        // Initial computation
        this.updateComputedFields(schema.fields, "");
    }

    buildField(key, fieldDef, path) {
        const type = fieldDef.type;
        const value = this.state.getValue(this.state.currentSchemaId, path);

        if (type === 'group') {
            return this.buildGroup(key, fieldDef, path);
        } else if (type === 'array') {
            return this.buildArray(key, fieldDef, path);
        }

        const group = document.createElement('div');
        group.className = 'form-group';
        
        // Make textareas full width natively
        if (type === 'textarea' || type === 'tags' || type === 'computed') {
            group.classList.add('full-width');
        }

        let labelText = fieldDef.label || this.formatLabel(key);
        const label = document.createElement('label');
        label.innerHTML = `${labelText} ${fieldDef.required ? '<span class="required-asterisk">*</span>' : ''}`;
        group.appendChild(label);

        let input;

        switch (type) {
            case 'scale': {
                const rangeContainer = document.createElement('div');
                rangeContainer.className = 'range-container';
                
                input = document.createElement('input');
                input.type = 'range';
                input.className = 'range-input';
                input.min = fieldDef.min !== undefined ? fieldDef.min : 0;
                input.max = fieldDef.max || 10;
                input.value = value !== undefined ? value : Math.floor((input.max - input.min) / 2) + parseInt(input.min);
                input.required = !!fieldDef.required;

                const badge = document.createElement('span');
                badge.className = 'range-badge';
                badge.textContent = input.value;

                input.addEventListener('input', (e) => {
                    badge.textContent = e.target.value;
                    let val = parseFloat(e.target.value);
                    
                    // Color shift logic (simple linear calculation between red(0)/green(120) hues)
                    // If it's a stress/bad metric, usually lower is better, but it's context dependent. We'll default to amber.
                    
                    this.state.setValue(path, val);
                });
                
                if (value === undefined) {
                    this.state.setValue(path, parseFloat(input.value));
                }

                rangeContainer.appendChild(input);
                rangeContainer.appendChild(badge);
                group.appendChild(rangeContainer);
                break;
            }
            case 'number':
                input = document.createElement('input');
                input.type = 'number';
                input.step = 'any';
                input.value = value !== undefined ? value : '';
                input.addEventListener('input', (e) => this.state.setValue(path, e.target.value ? parseFloat(e.target.value) : null));
                group.appendChild(input);
                break;
            case 'text':
                input = document.createElement('input');
                input.type = 'text';
                input.value = value || '';
                input.addEventListener('input', (e) => this.state.setValue(path, e.target.value));
                group.appendChild(input);
                break;
            case 'textarea':
                input = document.createElement('textarea');
                input.value = value || '';
                input.addEventListener('input', (e) => this.state.setValue(path, e.target.value));
                group.appendChild(input);
                break;
            case 'boolean': {
                const toggleContainer = document.createElement('label');
                toggleContainer.className = 'toggle-switch';
                
                input = document.createElement('input');
                input.type = 'checkbox';
                input.checked = !!value;
                input.addEventListener('change', (e) => this.state.setValue(path, e.target.checked));
                
                const slider = document.createElement('span');
                slider.className = 'toggle-slider';
                
                toggleContainer.appendChild(input);
                toggleContainer.appendChild(slider);
                group.appendChild(toggleContainer);
                break;
            }
            case 'select':
                input = document.createElement('select');
                const defaultOp = document.createElement('option');
                defaultOp.value = '';
                defaultOp.textContent = 'Select...';
                input.appendChild(defaultOp);
                
                if (fieldDef.options) {
                    fieldDef.options.forEach(opt => {
                        const option = document.createElement('option');
                        option.value = opt;
                        option.textContent = opt;
                        if (value === opt) option.selected = true;
                        input.appendChild(option);
                    });
                }
                input.addEventListener('change', (e) => this.state.setValue(path, e.target.value));
                group.appendChild(input);
                break;
            case 'time':
            case 'date':
            case 'datetime': {
                input = document.createElement('input');
                if (type === 'time') input.type = 'time';
                else if (type === 'date') input.type = 'date';
                else input.type = 'datetime-local';
                
                if (value) {
                    input.value = value;
                } else if (fieldDef.required || type === 'datetime' || type === 'date') {
                    // Default to current date/time
                    const now = new Date();
                    let defVal = '';
                    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
                    
                    if (type === 'date') {
                        defVal = now.toISOString().slice(0, 10);
                    } else if (type === 'datetime') {
                        defVal = now.toISOString().slice(0, 16);
                    } else {
                        defVal = now.toISOString().slice(11, 16);
                    }
                    input.value = defVal;
                    // Dont persist immediately to avoid empty submits with just dates, wait for user interact or download
                    this.state.setValue(path, defVal);
                }
                
                input.addEventListener('input', (e) => this.state.setValue(path, e.target.value));
                group.appendChild(input);
                break;
            }
            case 'tags': {
                const container = document.createElement('div');
                container.className = 'tags-input-container';
                
                let tags = Array.isArray(value) ? [...value] : [];
                if (!Array.isArray(value)) this.state.setValue(path, tags);
                
                const renderTags = () => {
                   container.innerHTML = '';
                   tags.forEach((tagText, idx) => {
                       const tag = document.createElement('div');
                       tag.className = 'tag';
                       tag.textContent = tagText;
                       
                       const removeBtn = document.createElement('button');
                       removeBtn.className = 'tag-remove';
                       removeBtn.innerHTML = '×';
                       removeBtn.onclick = (e) => {
                           e.preventDefault();
                           tags.splice(idx, 1);
                           this.state.setValue(path, tags);
                           renderTags();
                       };
                       
                       tag.appendChild(removeBtn);
                       container.appendChild(tag);
                   });
                   
                   const tagInput = document.createElement('input');
                   tagInput.type = 'text';
                   tagInput.className = 'tag-input';
                   tagInput.placeholder = fieldDef.placeholder || 'Type and press Enter...';
                   
                   tagInput.addEventListener('keydown', (e) => {
                       if (e.key === 'Enter' || e.key === ',') {
                           e.preventDefault();
                           const val = tagInput.value.trim().replace(/^,|,$/g, '');
                           if (val && !tags.includes(val)) {
                               tags.push(val);
                               this.state.setValue(path, tags);
                               renderTags();
                               container.querySelector('.tag-input').focus();
                           }
                       }
                   });
                   
                   container.appendChild(tagInput);
                };
                
                renderTags();
                group.appendChild(container);
                break;
            }
            case 'computed':
                input = document.createElement('div');
                input.className = 'computed-value';
                input.id = `computed-${path.replace(/\./g, '-')}`;
                input.textContent = value || '—';
                // Computed fields are calculated elsewhere
                group.appendChild(input);
                break;
        }

        if (input && input.tagName !== 'DIV') {
             input.id = `field-${path.replace(/\./g, '-')}`;
        }

        if (input && fieldDef.required && input.tagName === 'INPUT') {
             input.required = true;
        }

        return group;
    }

    buildGroup(key, fieldDef, path) {
        const section = document.createElement('div');
        section.className = 'form-section full-width';
        
        const title = document.createElement('h3');
        title.className = 'section-title';
        title.textContent = fieldDef.label || this.formatLabel(key);
        section.appendChild(title);
        
        const grid = document.createElement('div');
        grid.className = 'form-grid';
        
        for (const [subKey, subField] of Object.entries(fieldDef.fields)) {
            grid.appendChild(this.buildField(subKey, subField, `${path}.${subKey}`));
        }
        
        section.appendChild(grid);
        return section;
    }

    buildArray(key, fieldDef, path) {
        const group = document.createElement('div');
        group.className = 'form-group full-width array-container';
        
        const label = document.createElement('label');
        label.innerHTML = `${fieldDef.label || this.formatLabel(key)} ${fieldDef.required ? '<span class="required-asterisk">*</span>' : ''}`;
        group.appendChild(label);
        
        const itemsContainer = document.createElement('div');
        itemsContainer.className = 'array-items-wrapper';
        group.appendChild(itemsContainer);
        
        let items = this.state.getValue(this.state.currentSchemaId, path);
        if (!Array.isArray(items)) {
            items = [];
            this.state.setValue(path, items);
        }
        
        const renderItems = () => {
            items = this.state.getValue(this.state.currentSchemaId, path) || [];
            itemsContainer.innerHTML = '';
            
            items.forEach((item, index) => {
                const itemCard = document.createElement('div');
                itemCard.className = 'array-item';
                
                const itemHeader = document.createElement('div');
                itemHeader.className = 'array-item-header';
                
                const itemTitle = document.createElement('div');
                itemTitle.className = 'array-item-title';
                itemTitle.textContent = `Entry #${index + 1}`;
                
                const removeBtn = document.createElement('button');
                removeBtn.className = 'btn btn-danger';
                removeBtn.innerHTML = '✕ Remove';
                removeBtn.onclick = (e) => {
                    e.preventDefault();
                    this.state.removeArrayItem(path, index);
                    renderItems();
                };
                
                itemHeader.appendChild(itemTitle);
                itemHeader.appendChild(removeBtn);
                itemCard.appendChild(itemHeader);
                
                const grid = document.createElement('div');
                grid.className = 'form-grid';
                
                for (const [subKey, subField] of Object.entries(fieldDef.itemFields)) {
                    grid.appendChild(this.buildField(subKey, subField, `${path}.${index}.${subKey}`));
                }
                
                itemCard.appendChild(grid);
                itemsContainer.appendChild(itemCard);
            });
        };
        
        renderItems();
        
        const addBtn = document.createElement('button');
        addBtn.className = 'btn-add-array';
        addBtn.innerHTML = '+ Add Entry';
        addBtn.onclick = (e) => {
            e.preventDefault();
            const currentItems = this.state.getValue(this.state.currentSchemaId, path) || [];
            currentItems.push({});
            this.state.setValue(path, currentItems);
            renderItems();
        };
        
        group.appendChild(addBtn);
        return group;
    }
    
    updateComputedFields(fields, basePath) {
        if (!this.state.currentSchemaId) return;
        
        for (const [key, fieldDef] of Object.entries(fields)) {
            const path = basePath ? `${basePath}.${key}` : key;
            
            if (fieldDef.type === 'group') {
                this.updateComputedFields(fieldDef.fields, path);
            } else if (fieldDef.type === 'computed') {
                try {
                    let result = null;
                    
                    if (fieldDef.formula && fieldDef.formula.includes('-')) {
                         const parts = fieldDef.formula.split('-').map(p => p.trim());
                         if (parts.length === 2) {
                             // Check if either part is a numeric constant
                             const aIsNum = !isNaN(Number(parts[0]));
                             const bIsNum = !isNaN(Number(parts[1]));
                             
                             const a = aIsNum ? Number(parts[0]) : this.state.getValue(this.state.currentSchemaId, `${basePath ? basePath + '.' : ''}${parts[0]}`);
                             const b = bIsNum ? Number(parts[1]) : this.state.getValue(this.state.currentSchemaId, `${basePath ? basePath + '.' : ''}${parts[1]}`);
                             
                             if (a !== undefined && a !== null && b !== undefined && b !== null) {
                                 result = Number(a) - Number(b);
                             }
                         }
                    }
                    
                    if (result !== null) {
                        this.state.setValue(path, result);
                        const el = document.getElementById(`computed-${path.replace(/\./g, '-')}`);
                        if (el) el.textContent = result;
                    }
                } catch(e) {
                    console.error("Error computing field:", path, e);
                }
            }
        }
    }

    // Generic duration calculator for session-based schemas
    runDurationCalculations(schemaId, startField, endField, durationField) {
        const parseDt = (dtStr) => dtStr ? new Date(dtStr) : null;
        
        const startDt = parseDt(this.state.getValue(schemaId, startField));
        const endDt = parseDt(this.state.getValue(schemaId, endField));
        
        if (startDt && endDt && endDt > startDt) {
            const diffMins = Math.round((endDt - startDt) / 60000);
            const currentVal = this.state.getValue(schemaId, durationField);
            if (currentVal !== diffMins) {
                this.state.setValue(durationField, diffMins);
                const el = document.getElementById(`field-${durationField.replace(/\./g, '-')}`);
                if (el && document.activeElement !== el) {
                    el.value = diffMins;
                }
            }
        }
    }

    // Screen time: autopilot = 100 - intentional
    runScreentimeCalculations() {
        // Handled by updateComputedFields since autopilot_percent is now a computed field
        // No extra logic needed here beyond what updateComputedFields does
    }

    // Work session: sum pauses, calculate productive time
    runWorkSessionCalculations() {
        const updateVal = (path, val) => {
            if (this.state.getValue('work_session', path) !== val) {
                this.state.setValue(path, val);
                const el = document.getElementById(`field-${path.replace(/\./g, '-')}`);
                if (el && document.activeElement !== el) {
                    el.value = val;
                }
            }
        };

        // Sum pause durations
        let totalPauseMins = 0;
        const pauses = this.state.getValue('work_session', 'pauses') || [];
        pauses.forEach((pause, idx) => {
            if (pause.pause_start_minutes !== undefined && pause.pause_end_minutes !== undefined &&
                pause.pause_start_minutes !== null && pause.pause_end_minutes !== null) {
                const dur = Math.max(0, Number(pause.pause_end_minutes) - Number(pause.pause_start_minutes));
                updateVal(`pauses.${idx}.duration_minutes`, dur);
                totalPauseMins += dur;
            } else if (pause.duration_minutes !== undefined && pause.duration_minutes !== null) {
                totalPauseMins += Number(pause.duration_minutes);
            }
        });
        updateVal('total_pause_minutes', totalPauseMins);

        // Productive time = duration - pauses
        const duration = this.state.getValue('work_session', 'duration_minutes');
        if (duration !== undefined && duration !== null) {
            const productive = Math.max(0, Number(duration) - totalPauseMins);
            updateVal('total_productive_minutes', productive);
        }
    }

    // Helper: format float hours as "Xhr Ymin"
    formatHoursToHrMin(floatHours) {
        const totalMins = Math.round(Math.abs(floatHours) * 60);
        const hrs = Math.floor(totalMins / 60);
        const mins = totalMins % 60;
        const sign = floatHours < 0 ? '-' : '';
        return `${sign}${hrs}hr ${mins}min`;
    }

    runSleepCalculations() {
        const updateVal = (path, val) => {
            if (this.state.getValue('sleep', path) !== val) {
                this.state.setValue(path, val);
                const el = document.getElementById(`field-${path.replace(/\./g, '-')}`);
                if (el && document.activeElement !== el) {
                    el.value = val;
                }
            }
        };

        let targetHours = localStorage.getItem('target_sleep_hours') || "8";
        let targetSleep = localStorage.getItem('target_sleep_time') || "22:00";
        let targetWake = localStorage.getItem('target_wake_time') || "06:00";
        
        let stateTargetHours = this.state.getValue('sleep', 'target_sleep_hours');
        let stateTargetSleep = this.state.getValue('sleep', 'target_sleep_time');
        let stateTargetWake = this.state.getValue('sleep', 'target_wake_time');

        if (stateTargetHours && String(stateTargetHours) !== targetHours) {
            localStorage.setItem('target_sleep_hours', String(stateTargetHours));
            targetHours = String(stateTargetHours);
        } else if (!stateTargetHours) {
            updateVal('target_sleep_hours', Number(targetHours));
        }

        if (stateTargetSleep && stateTargetSleep !== targetSleep) {
            localStorage.setItem('target_sleep_time', stateTargetSleep);
            targetSleep = stateTargetSleep;
        } else if (!stateTargetSleep) {
            updateVal('target_sleep_time', targetSleep);
        }

        if (stateTargetWake && stateTargetWake !== targetWake) {
            localStorage.setItem('target_wake_time', stateTargetWake);
            targetWake = stateTargetWake;
        } else if (!stateTargetWake) {
            updateVal('target_wake_time', targetWake);
        }

        const timeToMins = (tStr) => {
            if (!tStr) return 0;
            const parts = tStr.split(':');
            return Number(parts[0]) * 60 + Number(parts[1]);
        };
        const parseDatetime = (dtStr) => {
             if (!dtStr) return null;
             return new Date(dtStr);
        };

        let totalInterruptionMins = 0;
        const interruptions = this.state.getValue('sleep', 'interruptions') || [];
        interruptions.forEach((intrp, idx) => {
            if (intrp.wake_time && intrp.back_to_sleep_time) {
                let w = timeToMins(intrp.wake_time);
                let b = timeToMins(intrp.back_to_sleep_time);
                let dur = b - w;
                if (dur < 0) dur += 24 * 60; 
                updateVal(`interruptions.${idx}.duration_minutes`, dur);
                totalInterruptionMins += dur;
            }
        });
        updateVal('total_interruption_minutes', totalInterruptionMins);

        const startDt = parseDatetime(this.state.getValue('sleep', 'sleep_start'));
        const endDt = parseDatetime(this.state.getValue('sleep', 'sleep_end'));
        
        if (startDt && endDt) {
            let diffMins = Math.max(0, (endDt - startDt) / 60000) - totalInterruptionMins;
            let totalSleepMins = Math.max(0, diffMins);
            let totalHoursStr = (totalSleepMins / 60).toFixed(2);
            updateVal('total_sleep_hours', Number(totalHoursStr));
        }

        if (startDt && targetSleep) {
            let startMins = startDt.getHours() * 60 + startDt.getMinutes();
            let targetMins = timeToMins(targetSleep);
            let diff = startMins - targetMins;
            if (diff > 12 * 60) diff -= 24 * 60;
            if (diff < -12 * 60) diff += 24 * 60;
            updateVal('sleep_start_delta_minutes', diff);
        }

        if (endDt && targetWake) {
            let wakeMins = endDt.getHours() * 60 + endDt.getMinutes();
            let targetMins = timeToMins(targetWake);
            let diff = wakeMins - targetMins;
            if (diff > 12 * 60) diff -= 24 * 60;
            if (diff < -12 * 60) diff += 24 * 60;
            updateVal('wake_time_delta_minutes', diff);
        }

        let actualSleep = this.state.getValue('sleep', 'total_sleep_hours');
        if (actualSleep !== undefined && actualSleep !== null) {
            let th = Number(targetHours);
            let deltaHours = actualSleep - th;
            updateVal('sleep_hours_delta', Number(deltaHours.toFixed(2)));
            updateVal('sleep_debt_hours', Number((-deltaHours).toFixed(2)));

            // Show human-readable format next to the sleep_hours_delta field
            const deltaEl = document.getElementById('field-sleep_hours_delta');
            if (deltaEl) {
                let displaySpan = deltaEl.parentElement.querySelector('.sleep-delta-display');
                if (!displaySpan) {
                    displaySpan = document.createElement('span');
                    displaySpan.className = 'sleep-delta-display';
                    displaySpan.style.cssText = 'margin-left: 8px; font-size: 0.85em; color: var(--text-secondary, #aaa); font-weight: 500;';
                    deltaEl.parentElement.appendChild(displaySpan);
                }
                displaySpan.textContent = `(${this.formatHoursToHrMin(deltaHours)})`;
            }
        }

        let totalNapMins = 0;
        const naps = this.state.getValue('sleep', 'naps') || [];
        naps.forEach((nap, idx) => {
            if (nap.start_time && nap.end_time) {
                let s = timeToMins(nap.start_time);
                let e = timeToMins(nap.end_time);
                let dur = e - s;
                if (dur < 0) dur += 24 * 60;
                updateVal(`naps.${idx}.duration_minutes`, dur);
                totalNapMins += dur;
            }
        });
        updateVal('total_nap_minutes', totalNapMins);

        const dreams = this.state.getValue('sleep', 'dreams') || [];
        updateVal('dream_count', dreams.length);
    }
}
