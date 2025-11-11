# Desktop Web App - AFTERPACKETS

## Setup

### Install Dependencies
```bash
npm install
```

### Development
```bash
npm run dev
# Opens http://localhost:3000
```

### Build for Production
```bash
npm run build
# Output in dist/
```

### Preview Production Build
```bash
npm run preview
```

## Features

### Dashboard
- Import evidence bundles (.zip or .json)
- View protocol distribution (pie chart)
- Monitor top talkers
- Browse recent packets

### Map View
- Geo-location of remote hosts
- Interactive markers with packet stats
- Country/city information

### Replay
- Timeline playback
- Frame-by-frame analysis
- Packet rate visualization

### Rules
- Create custom detection rules
- Enable/disable rules
- Export rule configurations

## Tech Stack
- React 18 + TypeScript
- Vite (build tool)
- Tailwind CSS
- Recharts (charts)
- Leaflet (maps)
- Zustand (state)
- React Router

## File Structure
```
src/
├── pages/       # Route components
├── store/       # Zustand store
├── types/       # TypeScript definitions
├── App.tsx      # Main app component
└── main.tsx     # Entry point
```
