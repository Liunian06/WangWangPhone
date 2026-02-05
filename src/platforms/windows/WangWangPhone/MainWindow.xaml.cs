using System;
using System.Management;
using System.Windows;
using System.Windows.Threading;
using System.Threading.Tasks;

namespace WangWangPhone
{
    public partial class MainWindow : Window
    {
        private DispatcherTimer _timer;

        public MainWindow()
        {
            InitializeComponent();
            SetupTimer();
            UpdateDateTime();
            _ = LoadWeatherData();
            LoadMachineId();
        }

        private void LoadMachineId()
        {
            try
            {
                string cpuId = string.Empty;
                using (var mc = new ManagementClass("win32_processor"))
                {
                    foreach (var mo in mc.GetInstances())
                    {
                        cpuId = mo.Properties["ProcessorId"].Value.ToString();
                        break;
                    }
                }
                MachineIdTextBox.Text = string.IsNullOrEmpty(cpuId) ? "WIN-UNKNOWN-ID" : cpuId;
            }
            catch
            {
                MachineIdTextBox.Text = "WIN-ACCESS-DENIED";
            }
        }

        private void SetupTimer()
        {
            _timer = new DispatcherTimer();
            _timer.Interval = TimeSpan.FromSeconds(1);
            _timer.Tick += (s, e) => UpdateDateTime();
            _timer.Start();
        }

        private void UpdateDateTime()
        {
            var now = DateTime.Now;
            TimeText.Text = now.ToString("HH:mm");
            DateText.Text = now.ToString("M月d日 dddd", System.Globalization.CultureInfo.GetCultureInfo("zh-CN"));
        }

        private async Task LoadWeatherData()
        {
            // Simulate Network Delay
            await Task.Delay(500);

            // Mock Data
            string city = "广州";
            string temp = "25°";
            string desc = "多云";
            string icon = "⛅";
            string range = "H:29° L:21°";

            // Update UI
            ClockCityText.Text = city;
            WeatherCityText.Text = city;
            WeatherTempText.Text = temp;
            WeatherIconText.Text = icon;
            WeatherDescText.Text = desc;
            WeatherRangeText.Text = range;
        }

        private void OnSettingsClick(object sender, System.Windows.Input.MouseButtonEventArgs e)
        {
            SettingsOverlay.Visibility = Visibility.Visible;
        }

        private void OnBackClick(object sender, RoutedEventArgs e)
        {
            SettingsOverlay.Visibility = Visibility.Collapsed;
        }

        private void OnActivationClick(object sender, System.Windows.Input.MouseButtonEventArgs e)
        {
            ActivationOverlay.Visibility = Visibility.Visible;
        }

        private void OnActivationClickUI(object sender, RoutedEventArgs e)
        {
            ActivationOverlay.Visibility = Visibility.Visible;
        }

        private void OnActivationBackClick(object sender, RoutedEventArgs e)
        {
            ActivationOverlay.Visibility = Visibility.Collapsed;
        }

        private void OnCopyMachineId(object sender, RoutedEventArgs e)
        {
            Clipboard.SetText(MachineIdTextBox.Text);
        }

        private void OnPasteLicenseKey(object sender, RoutedEventArgs e)
        {
            if (Clipboard.ContainsText())
            {
                LicenseKeyTextBox.Text = Clipboard.GetText();
            }
        }

        protected override void OnKeyDown(System.Windows.Input.KeyEventArgs e)
        {
            if (e.Key == System.Windows.Input.Key.Escape)
            {
                if (ActivationOverlay.Visibility == Visibility.Visible)
                {
                    ActivationOverlay.Visibility = Visibility.Collapsed;
                    e.Handled = true;
                }
                else if (SettingsOverlay.Visibility == Visibility.Visible)
                {
                    SettingsOverlay.Visibility = Visibility.Collapsed;
                    e.Handled = true;
                }
            }
            base.OnKeyDown(e);
        }

        private void OnActivateSubmit(object sender, RoutedEventArgs e)
        {
            string licenseKey = LicenseKeyTextBox.Text;
            string mid = MachineIdTextBox.Text;

            // 统一模拟校验逻辑：必须以 WANGWANG- 开头
            if (!string.IsNullOrEmpty(licenseKey) && licenseKey.StartsWith("WANGWANG-"))
            {
                ActivationStatusText.Text = "已查看 >";
                ExpiryDateText.Text = "有效期至: 2030-01-01";
                ExpiryDateText.Visibility = Visibility.Visible;
                MessageBox.Show("软件激活成功！", "授权管理", MessageBoxButton.OK, MessageBoxImage.Information);
                ActivationOverlay.Visibility = Visibility.Collapsed;
            }
            else
            {
                MessageBox.Show("激活码格式错误或无效，请检查后重试。", "错误", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }
    }
}