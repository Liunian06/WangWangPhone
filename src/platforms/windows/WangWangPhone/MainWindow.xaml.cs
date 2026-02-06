using System;
using System.Management;
using System.Windows;
using System.Windows.Threading;
using System.Threading.Tasks;
using WangWangPhone.Core;
using System.Collections.Generic;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;

namespace WangWangPhone
{
    public class AppIconData
    {
        public string Id { get; set; }
        public string Name { get; set; }
        public string Icon { get; set; }
        public int Col { get; set; }
        public int Row { get; set; }
        public bool UseImage { get; set; }
    }

    public partial class MainWindow : Window
    {
        private DispatcherTimer _timer;
        private readonly LicenseManager _licenseManager = LicenseManager.Instance;
        private List<AppIconData> _apps;
        private UIElement _draggedElement;
        private Point _dragStart;
        private AppIconData _draggedApp;

        public MainWindow()
        {
            InitializeComponent();
            SetupTimer();
            UpdateDateTime();
            _ = LoadWeatherData();
            InitializeLicense();
            InitializeApps();
        }

        private void InitializeApps()
        {
            _apps = new List<AppIconData>
            {
                new AppIconData { Id = "phone", Name = "电话", Icon = "📞", Col = 0, Row = 0 },
                new AppIconData { Id = "msg", Name = "信息", Icon = "💬", Col = 1, Row = 0 },
                new AppIconData { Id = "music", Name = "音乐", Icon = "🎵", Col = 2, Row = 0 },
                new AppIconData { Id = "camera", Name = "相机", Icon = "📷", Col = 3, Row = 0 },
                new AppIconData { Id = "settings", Name = "设置", Icon = "Assets/Setting_Light.png", UseImage = true, Col = 0, Row = 1 },
                new AppIconData { Id = "wangwang", Name = "汪汪", Icon = "🐶", Col = 1, Row = 1 }
            };

            // 加载保存的布局
            var savedLayouts = _licenseManager.GetAppLayouts();
            foreach (var layout in savedLayouts)
            {
                var app = _apps.Find(a => a.Id == layout.AppId);
                if (app != null)
                {
                    app.Col = layout.Col;
                    app.Row = layout.Row;
                }
            }

            RenderApps();
        }

        private void RenderApps()
        {
            AppCanvas.Children.Clear();
            foreach (var app in _apps)
            {
                var appItem = CreateAppItem(app);
                AppCanvas.Children.Add(appItem);
                UpdateElementPosition(appItem, app.Col, app.Row);
            }
        }

        private UIElement CreateAppItem(AppIconData app)
        {
            var stack = new StackPanel { Width = 80, Margin = new Thickness(5), Tag = app };
            var border = new Border { Width = 80, Height = 80 };
            
            if (app.UseImage)
            {
                border.Child = new Image { Source = new System.Windows.Media.Imaging.BitmapImage(new Uri(app.Icon, UriKind.RelativeOrAbsolute)), Width = 80, Height = 80 };
            }
            else
            {
                border.Child = new TextBlock { Text = app.Icon, FontSize = 65, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center };
            }

            stack.Children.Add(border);
            stack.Children.Add(new TextBlock { Text = app.Name, Foreground = Brushes.White, FontSize = 12, HorizontalAlignment = HorizontalAlignment.Center, Margin = new Thickness(0, 5, 0, 0) });

            stack.MouseLeftButtonDown += OnAppMouseDown;
            stack.MouseMove += OnAppMouseMove;
            stack.MouseLeftButtonUp += OnAppMouseUp;

            return stack;
        }

        private void OnAppMouseDown(object sender, MouseButtonEventArgs e)
        {
            _draggedElement = sender as UIElement;
            _dragStart = e.GetPosition(AppCanvas);
            _draggedApp = (sender as StackPanel).Tag as AppIconData;
            _draggedElement.CaptureMouse();
            Panel.SetZIndex(_draggedElement, 1000);
        }

        private void OnAppMouseMove(object sender, MouseEventArgs e)
        {
            if (_draggedElement != null)
            {
                Point current = e.GetPosition(AppCanvas);
                double left = Canvas.GetLeft(_draggedElement) + (current.X - _dragStart.X);
                double top = Canvas.GetTop(_draggedElement) + (current.Y - _dragStart.Y);
                
                Canvas.SetLeft(_draggedElement, left);
                Canvas.SetTop(_draggedElement, top);
                _dragStart = current;
            }
        }

        private void OnAppMouseUp(object sender, MouseButtonEventArgs e)
        {
            if (_draggedElement != null)
            {
                _draggedElement.ReleaseMouseCapture();
                
                // 计算落点网格
                double left = Canvas.GetLeft(_draggedElement);
                double top = Canvas.GetTop(_draggedElement);
                
                int col = (int)Math.Round(left / 100);
                int row = (int)Math.Round(top / 100);
                
                _draggedApp.Col = Math.Max(0, Math.Min(col, 3));
                _draggedApp.Row = Math.Max(0, Math.Min(row, 5));
                
                UpdateElementPosition(_draggedElement, _draggedApp.Col, _draggedApp.Row);
                
                // 持久化
                _licenseManager.SaveAppLayout(_draggedApp.Id, _draggedApp.Col, _draggedApp.Row);
                
                if (left < 5 && top < 5 && _draggedApp.Id == "settings") {
                    OnSettingsClick(null, null);
                }

                _draggedElement = null;
                _draggedApp = null;
            }
        }

        private void UpdateElementPosition(UIElement element, int col, int row)
        {
            Canvas.SetLeft(element, col * 100);
            Canvas.SetTop(element, row * 100);
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