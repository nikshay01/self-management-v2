// js/state.js

/**
 * Manages the in-memory state of all forms.
 * Prevents data loss when switching between schemas.
 */
class StateManager {
  constructor() {
    this.store = {};
    this.currentSchemaId = null;
    this.listeners = [];
  }

  // Initialize a schema state if it doesn't exist
  initSchema(schemaId) {
    if (!this.store[schemaId]) {
      this.store[schemaId] = {};
    }
  }

  setCurrentSchema(schemaId) {
    this.currentSchemaId = schemaId;
    this.importedFilename = null;
    this.initSchema(schemaId);
  }

  // Set a value at a specific path, e.g. "measurements.arms.left_bicep_cm" or "goals.0.title"
  setValue(path, value) {
    if (!this.currentSchemaId) return;

    const parts = path.split('.');
    let current = this.store[this.currentSchemaId];

    for (let i = 0; i < parts.length - 1; i++) {
        const part = parts[i];
        if (!current[part]) {
            // Check next part to see if we should create an array or object
            const nextPart = parts[i+1];
            current[part] = /^\d+$/.test(nextPart) ? [] : {};
        }
        current = current[part];
    }

    current[parts[parts.length - 1]] = value;
    this._notifyListeners();
  }

  // Get a value from a specific path
  getValue(schemaId, path) {
    const parts = path.split('.');
    let current = this.store[schemaId];
    if (!current) return undefined;

    for (let i = 0; i < parts.length; i++) {
        if (current[parts[i]] === undefined) return undefined;
        current = current[parts[i]];
    }
    return current;
  }
  
  // Remove an item from an array (for repeatable fields)
  removeArrayItem(path, index) {
      if (!this.currentSchemaId) return;
      
      const parts = path.split('.');
      let current = this.store[this.currentSchemaId];
      
      for (let i = 0; i < parts.length; i++) {
          if (current[parts[i]] === undefined) return;
          current = current[parts[i]];
      }
      
      if (Array.isArray(current)) {
          current.splice(index, 1);
          this._notifyListeners();
      }
  }

  // Get the full state object for a schema
  getSchemaState(schemaId) {
    return this.store[schemaId] || {};
  }

  // Set the full state object for a schema (used for hydrating from imported JSON)
  setSchemaState(schemaId, data) {
    this.store[schemaId] = data || {};
    this.currentSchemaId = schemaId;
    this._notifyListeners();
  }

  subscribe(callback) {
      this.listeners.push(callback);
  }

  _notifyListeners() {
      this.listeners.forEach(cb => cb(this.store[this.currentSchemaId]));
  }
}

const state = new StateManager();
