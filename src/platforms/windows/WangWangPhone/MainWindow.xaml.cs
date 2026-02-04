using System;
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
    }
}