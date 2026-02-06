using System;
using System.Management;
using System.Windows;
using System.Windows.Threading;
using System.Threading.Tasks;
using WangWangPhone.Core;

namespace WangWangPhone
{
    public partial class MainWindow : Window
    {
        private DispatcherTimer _timer;
        private readonly LicenseManager _licenseManager = LicenseManager.Instance;

        public MainWindow()
        {
            InitializeComponent();
            SetupTimer();
            UpdateDateTime();
            _ = LoadWeatherData();
            InitializeLicense();
            ApplyThemeIcons();
        }

        private void ApplyThemeIcons()
        {
            // Simple logic to detect "dark" theme by checking a system color or just hardcode for this prototype
            // In WPF, we can use Registry or SystemParameters
            bool isDark = SystemParameters.HighContrast; // Placeholder logic
            
            // For now, let's just make it toggleable or check background
            // Actually, let's just default to Light since it's a prototype
            SettingsIconLight.Visibility = Visibility.Visible;
            SettingsIconDark.Visibility = Visibility.Collapsed;
        }

        /// <summary>
        /// 初始化授权管理器，从数据库恢复激活状态，并设置机器码
        /// </summary>
        private void InitializeLicense()
        {
            // 初始化 LicenseManager（内部会打开数据库并恢复缓存）
            _licenseManager.Initialize();

            // 显示机器码
            MachineIdTextBox.Text = _licenseManager.GetMachineId();

            // 从数据库恢复的激活状态刷新到 UI
            if (_licenseManager.IsActivated())
            {
                ActivationStatusText.Text = "已查看 >";
                ExpiryDateText.Text = $"有效期至: {_licenseManager.GetExpirationDateString()}";
                ExpiryDateText.Visibility = Visibility.Visible;
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

        private async void OnActivateSubmit(object sender, RoutedEventArgs e)
        {
            string licenseKey = LicenseKeyTextBox.Text?.Trim();

            if (string.IsNullOrEmpty(licenseKey))
            {
                MessageBox.Show("请输入激活码。", "提示", MessageBoxButton.OK, MessageBoxImage.Warning);
                return;
            }

            // 通过 LicenseManager 验证并持久化激活信息到数据库
            var result = await _licenseManager.VerifyLicenseAsync(licenseKey);

            if (result.IsSuccess)
            {
                ActivationStatusText.Text = "已查看 >";
                ExpiryDateText.Text = $"有效期至: {_licenseManager.GetExpirationDateString()}";
                ExpiryDateText.Visibility = Visibility.Visible;
                MessageBox.Show("软件激活成功！", "授权管理", MessageBoxButton.OK, MessageBoxImage.Information);
                ActivationOverlay.Visibility = Visibility.Collapsed;
            }
            else
            {
                MessageBox.Show($"激活失败: {result.ErrorMessage}", "错误", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }
    }
}