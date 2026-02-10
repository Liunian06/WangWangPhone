import SwiftUI

struct CalculatorAppView: View {
    @Binding var isPresented: Bool
    @State private var displayText = "0"
    @State private var operand1: Double? = nil
    @State private var operatorSymbol: String? = nil
    @State private var isWaitingForOperand = false
    
    let buttons = [
        ["AC", "+/-", "%", "÷"],
        ["7", "8", "9", "×"],
        ["4", "5", "6", "-"],
        ["1", "2", "3", "+"],
        ["0", ".", "="]
    ]
    
    var body: some View {
        VStack(spacing: 12) {
            Spacer()
            
            // Display
            HStack {
                Spacer()
                Text(displayText)
                    .font(.system(size: 80, weight: .light))
                    .foregroundColor(.white)
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 10)
            
            // Buttons Grid
            ForEach(buttons, id: \.self) { row in
                HStack(spacing: 12) {
                    ForEach(row, id: \.self) { label in
                        CalculatorButton(
                            label: label,
                            backgroundColor: buttonBackgroundColor(label),
                            foregroundColor: buttonForegroundColor(label),
                            isWide: label == "0"
                        ) {
                            buttonTapped(label)
                        }
                    }
                }
            }
        }
        .padding(.horizontal, 12)
        .padding(.bottom, 40)
        .background(Color.black)
        .overlay(
            VStack {
                HStack {
                    Button(action: { isPresented = false }) {
                        Image(systemName: "chevron.down")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(.white.opacity(0.6))
                            .padding()
                    }
                    Spacer()
                }
                Spacer()
            }
        )
    }
    
    private func buttonBackgroundColor(_ label: String) -> Color {
        if ["÷", "×", "-", "+", "="].contains(label) {
            return Color.orange
        } else if ["AC", "+/-", "%"].contains(label) {
            return Color(white: 0.65)
        } else {
            return Color(white: 0.2)
        }
    }
    
    private func buttonForegroundColor(_ label: String) -> Color {
        if ["AC", "+/-", "%"].contains(label) {
            return .black
        } else {
            return .white
        }
    }
    
    private func buttonTapped(_ label: String) {
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        
        switch label {
        case "0"..."9":
            if isWaitingForOperand || displayText == "0" {
                displayText = label
                isWaitingForOperand = false
            } else {
                displayText += label
            }
        case ".":
            if isWaitingForOperand {
                displayText = "0."
                isWaitingForOperand = false
            } else if !displayText.contains(".") {
                displayText += "."
            }
        case "AC":
            displayText = "0"
            operand1 = nil
            operatorSymbol = nil
            isWaitingForOperand = false
        case "+", "-", "×", "÷":
            let currentVal = Double(displayText) ?? 0
            if operand1 == nil {
                operand1 = currentVal
            } else if let op = operatorSymbol, !isWaitingForOperand {
                let result = performCalculation(operand1!, currentVal, op)
                displayText = formatResult(result)
                operand1 = result
            }
            operatorSymbol = label
            isWaitingForOperand = true
        case "=":
            let currentVal = Double(displayText) ?? 0
            if let op1 = operand1, let op = operatorSymbol {
                let result = performCalculation(op1, currentVal, op)
                displayText = formatResult(result)
                operand1 = nil
                operatorSymbol = nil
                isWaitingForOperand = true
            }
        case "+/-":
            if let val = Double(displayText) {
                displayText = formatResult(val * -1)
            }
        case "%":
            if let val = Double(displayText) {
                displayText = formatResult(val / 100.0)
            }
        default:
            break
        }
    }
    
    private func performCalculation(_ op1: Double, _ op2: Double, _ symbol: String) -> Double {
        switch symbol {
        case "+": return op1 + op2
        case "-": return op1 - op2
        case "×": return op1 * op2
        case "÷": return op2 != 0 ? op1 / op2 : 0
        default: return op2
        }
    }
    
    private func formatResult(_ result: Double) -> String {
        let formatter = NumberFormatter()
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 8
        formatter.numberStyle = .decimal
        formatter.usesGroupingSeparator = false
        return formatter.string(from: NSNumber(value: result)) ?? "\(result)"
    }
}

struct CalculatorButton: View {
    let label: String
    let backgroundColor: Color
    let foregroundColor: Color
    var isWide: Bool = false
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            ZStack {
                if isWide {
                    RoundedRectangle(cornerRadius: 45)
                        .fill(backgroundColor)
                        .frame(height: 80)
                } else {
                    Circle()
                        .fill(backgroundColor)
                        .frame(width: 80, height: 80)
                }
                
                Text(label)
                    .font(.system(size: 32, weight: .medium))
                    .foregroundColor(foregroundColor)
                    .padding(.leading, isWide ? 30 : 0)
                    .frame(maxWidth: isWide ? .infinity : 80, alignment: isWide ? .leading : .center)
            }
        }
        .frame(maxWidth: isWide ? .infinity : 80)
    }
}
