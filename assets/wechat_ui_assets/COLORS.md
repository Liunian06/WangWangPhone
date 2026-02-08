# WeChat UI Color Palette

Based on Figma export data.

## Brand Colors

- **Brand Green (Primary)**: `#07C160` (Light), `#06AD56` (Dark)
- **Brand/BG**: `#EDEDED` (Background Grey)

## Text Colors (FG)

- **FG-0 (Primary Text)**: `#000000` (Alpha 90% or `#191919` solid equivalent)
- **FG-1 (Secondary Text)**: `rgba(0,0,0, 0.5)` or `#808080`
- **FG-2 (Hint/Disabled)**: `#B2B2B2`

## Background Colors (BG)

- **BG-0 (Page Background)**: `#EDEDED`
- **BG-1 (Cell Background)**: `#FFFFFF`
- **BG-2 (Input Area)**: `#F7F7F7`
- **BG-3 (Search Bar)**: `#EDEDED`

## Chat Colors

- **Bubble Sent (Green)**: `#95EC69`
- **Bubble Received (White)**: `#FFFFFF`
- **Red Packet**: `#FA9D3B` (Orange) / `#EA4C89` (Red - actually varies, using standard Red) -> Figma shows Orange `#FA9D3B` for some elements, but Red Packet is usually `#D84E43`.
    - *Correction from Figma Image*: **Orange 100** is `#FA9D3B`.

## Functional Colors

- **Red (Notification/Delete)**: `#FA5151`
- **Blue (Link)**: `#576B95`
- **Separator/Divider**: `#D9D9D9` (Thickness 0.5dp) or `rgba(0,0,0,0.1)`

## Specific Palette (from Figma Image)

### BG
- **0**: `#EDEDED`
- **1**: `#F7F7F7`
- **2**: `#FFFFFF`
- **3**: `#F7F7F7`
- **4**: `#4C4C4C`
- **5**: `#FFFFFF`

### BLUE
- **100**: `#10AEFF` (Light Blue)

### BRAND (Green)
- **100**: `#07C160`

### FG (Foreground/Text)
- **0**: `#000000` (Opacity 90%) -> Effectively `#191919` on white
- **0.5**: `#000000` (Opacity 90%)
- **1**: `#000000` (Opacity 50%) -> Secondary text
- **2**: `#000000` (Opacity 30%) -> Disable/Hint
- **3**: `#000000` (Opacity 10%)
- **4**: `#000000` (Opacity 5%)

### SEPARATORS
- **Line**: `rgba(0,0,0, 0.1)`

---
*Note: Android and iOS implementations should reference these hex codes directly.*