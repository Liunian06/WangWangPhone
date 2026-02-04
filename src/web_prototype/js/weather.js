/**
 * Weather Service Module
 * Fetches weather data for a specific city.
 */
import { pinyin } from './libs/pinyin-pro.js';

const DB_NAME = 'WeatherDB';
const DB_VERSION = 1;
const STORE_NAME = 'weather_cache';

// Initialize IndexedDB
function openDB() {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open(DB_NAME, DB_VERSION);
        request.onupgradeneeded = (event) => {
            const db = event.target.result;
            if (!db.objectStoreNames.contains(STORE_NAME)) {
                db.createObjectStore(STORE_NAME, { keyPath: 'city' });
            }
        };
        request.onsuccess = (event) => resolve(event.target.result);
        request.onerror = (event) => reject(event.target.error);
    });
}

// Get from Cache
async function getCachedWeather(city) {
    try {
        const db = await openDB();
        return new Promise((resolve, reject) => {
            const transaction = db.transaction([STORE_NAME], 'readonly');
            const store = transaction.objectStore(STORE_NAME);
            const request = store.get(city);
            request.onsuccess = () => {
                const data = request.result;
                if (data && (Date.now() - data.timestamp < 24 * 60 * 60 * 1000)) {
                    resolve(data);
                } else {
                    resolve(null);
                }
            };
            request.onerror = () => resolve(null);
        });
    } catch (e) {
        console.error("DB Error", e);
        return null;
    }
}

// Save to Cache
async function cacheWeather(city, data) {
    try {
        const db = await openDB();
        const transaction = db.transaction([STORE_NAME], 'readwrite');
        const store = transaction.objectStore(STORE_NAME);
        store.put({ city, ...data, timestamp: Date.now() });
    } catch (e) {
        console.error("DB Save Error", e);
    }
}

/**
 * Gets weather information for a city.
 * @param {string} city Chinese city name
 * @returns {Promise<object>} Weather data
 */
export async function getWeather(city) {
    // 1. Pinyin Conversion
    // Remove "市" if present for cleaner lookup if needed, but pinyin lib might handle it or we map it manually in mock
    const cleanCity = city.replace('市', '');
    const cityPinyin = pinyin(cleanCity, { toneType: 'none' }).replace(/\s+/g, ''); // "Beijing"

    console.log(`Weather: Querying for ${city} (${cityPinyin})`);

    // 2. Cache Check
    const cached = await getCachedWeather(cleanCity);
    if (cached) {
        console.log('Weather: Using cache');
        return cached;
    }

    // 3. API Request
    try {
        const response = await fetch(`https://goweather.herokuapp.com/weather/${cityPinyin}`);
        if (!response.ok) throw new Error('Network response was not ok');
        const data = await response.json();
        
        // Enhance data if needed (e.g. description mapping to icons)
        // For now, pass through.
        
        // 4. Persistence
        await cacheWeather(cleanCity, data);
        
        return { ...data, timestamp: Date.now() };
    } catch (e) {
        console.error("Weather fetch failed:", e);
        // Return dummy data on failure to prevent UI breaking
        return {
            temperature: "--",
            wind: "--",
            description: "Unavailable",
            forecast: []
        };
    }
}