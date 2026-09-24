// js/app.js

document.addEventListener('DOMContentLoaded', () => {
    const nav = document.getElementById('schema-nav');
    const bottomActions = document.getElementById('bottom-actions');
    const menuToggle = document.querySelector('.mobile-only-toggle');
    const sidebar = document.querySelector('.sidebar');
    const datetimeDisplay = document.getElementById('current-datetime');
    const mobileSelect = document.getElementById('mobile-schema-selector');
    
    // Initialize Form Builder
    const formBuilder = new FormBuilder(state);

    const selectSchema = (schemaId) => {
        // Update Sidebar Active State
        document.querySelectorAll('.nav-item').forEach(el => {
            if(el.dataset.schema === schemaId) el.classList.add('active');
            else el.classList.remove('active');
        });
        
        // Update Mobile Select state
        if (mobileSelect.value !== schemaId) {
            mobileSelect.value = schemaId;
        }
        
        // Set State and Build Form
        state.setCurrentSchema(schemaId);
        formBuilder.buildForm(schemaId);
        
        // Show bottom actions
        bottomActions.style.display = 'flex';
        
        // Close mobile menu if open
        if (window.innerWidth <= 768) {
            sidebar.classList.remove('open');
        }
        
        // Dispatch Custom Event so download.js knows schema changed
        window.dispatchEvent(new CustomEvent('schemaChanged', { detail: { schemaId } }));
    };

    // Listen for schemaHydrated to rebuild form
    window.addEventListener('schemaHydrated', () => {
        if (state.currentSchemaId) {
            formBuilder.buildForm(state.currentSchemaId);
        }
    });

    // Render Navigation
    Object.keys(SCHEMAS).forEach(schemaId => {
        const schema = SCHEMAS[schemaId];
        
        // Create Sidebar Button
        const btn = document.createElement('button');
        btn.className = 'nav-item';
        btn.textContent = schema.label;
        btn.dataset.schema = schemaId;
        
        btn.addEventListener('click', () => selectSchema(schemaId));
        nav.appendChild(btn);
        
        // Create Mobile Option
        const opt = document.createElement('option');
        opt.value = schemaId;
        opt.textContent = schema.label;
        mobileSelect.appendChild(opt);
    });
    
    // Listen to Mobile Select changes
    mobileSelect.addEventListener('change', (e) => {
        selectSchema(e.target.value);
    });

    // Mobile Menu Toggle
    if (menuToggle) {
        menuToggle.addEventListener('click', () => {
            sidebar.classList.toggle('open');
        });
    }

    // Live Date/Time Display
    const updateDateTime = () => {
        const now = new Date();
        const options = { 
            weekday: 'short', 
            year: 'numeric', 
            month: 'short', 
            day: 'numeric',
            hour: '2-digit', 
            minute: '2-digit',
            second: '2-digit'
        };
        datetimeDisplay.textContent = now.toLocaleDateString(undefined, options);
    };
    
    updateDateTime();
    setInterval(updateDateTime, 1000);

    // Theme selector
    const bgSelector = document.getElementById('bg-selector');
    
    // Setup initial background
    document.body.classList.add('bg-blueprint');

    bgSelector.addEventListener('change', (e) => {
        // Remove all bg- classes
        document.body.classList.forEach(cls => {
            if (cls.startsWith('bg-')) {
                document.body.classList.remove(cls);
            }
        });
        
        // Add new background class
        document.body.classList.add(`bg-${e.target.value}`);
    });
});
