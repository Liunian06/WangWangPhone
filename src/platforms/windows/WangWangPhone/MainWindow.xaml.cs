
using System;
using System.Collections.Generic;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Imaging;
using System.Windows.Threading;
using System.Threading.Tasks;
using WangWangPhone.Core;

namespace WangWangPhone
{
    public class AppIconModel
    {
        public string Id { get; set; }
        public string Name { get; set; }
        public string Icon { get; set; }
        public bool UseImage { get; set; }
    }

    public partial class MainWindow : Window
    {
        private DispatcherTimer _timer;
        private readonly LicenseManager _licenseManager = LicenseManager.Instance;
        private readonly LayoutManager _layoutManager = LayoutManager.Instance;

        private List<AppIconModel> _apps = new List<AppIconModel>();
        private List<AppIconModel> _dockApps = new List<AppIconModel>();
        private const int MaxDockApps = 4;

        // 小组件顺序
        private List<string> _widgetOrder = new List<string> { "clock", "weather" };

        // 编辑模式
        private bool _isEditMode = false;
        private DispatcherTimer _longPressTimer;
        private StackPanel _longPressTarget;
        private Point _longPressStartPos;

        // 拖拽
        private bool _isDragging = false;
        private StackPanel _draggedElement;
        private int _draggedIndex = -1;
        private Point _dragStartPoint;
        private string _dragSource = "grid";

        // 拖拽浮层
        private FrameworkElement _dragOverlayElement;

        // 小组件拖拽
        private bool _isWidgetDragging = false;
        private Border _draggedWidget;
        private Point _widgetDragStartPos;
        private DispatcherTimer _widgetLongPressTimer;

        private const int Columns = 4;
        private const double CellWidth = 85;
        private const double CellHeight = 105;

        private List<Storyboard> _wiggleStoryboards = new List<Storyboard>();

        public MainWindow()
        {
            InitializeComponent();
            SetupTimer();
            UpdateDateTime();
            _ = LoadWeatherData();
            InitializeLicense();
            InitializeLayout();
            ApplyThemeIcons();
            ApplyWallpapers();
            ApplyWidgetOrder();

            _widgetLongPressTimer = new DispatcherTimer();
            _widgetLongPressTimer.Interval = TimeSpan.FromMilliseconds(500);
            _widgetLongPressTimer.Tick += WidgetLongPressTimer_Tick;

            ChatAppControl.OnCloseRequested += () =>
            {
                ChatAppOverlay.Visibility = Visibility.Collapsed;
            };
        }

        #region 初始化

        private void ApplyThemeIcons() { }

        private void InitializeLicense()
        {
            _licenseManager.Initialize();
            MachineIdTextBox.Text = _licenseManager.GetMachineId();
            if (_licenseManager.IsActivated())
            {
                ActivationStatusText.Text = "已查看 >";
                ExpiryDateText.Text = $"有效期至: {_licenseManager.GetExpirationDateString()}";
                ExpiryDateText.Visibility = Visibility.Visible;
            }
        }

        private void InitializeLayout()
        {
            _layoutManager.Initialize();
            var defaultApps = GetDefaultApps();
            var savedLayout = _layoutManager.GetLayout();

            if (savedLayout.Count > 0)
            {
                var gridItems = savedLayout.Where(i => i.Area == "grid").OrderBy(i => i.Position).ToList();
                _apps.Clear();
                foreach (var li in gridItems)
                {
                    var app = defaultApps.FirstOrDefault(a => a.Id == li.AppId);
                    if (app != null) _apps.Add(app);
                }

                var dockItems = savedLayout.Where(i => i.Area == "dock").OrderBy(i => i.Position).ToList();
                _dockApps.Clear();
                foreach (var li in dockItems)
                {
                    var app = defaultApps.FirstOrDefault(a => a.Id == li.AppId);
                    if (app != null) _dockApps.Add(app);
                }

                var widgetItems = savedLayout.Where(i => i.Area == "widget").OrderBy(i => i.Position).ToList();
                if (widgetItems.Count > 0)
                    _widgetOrder = widgetItems.Select(i => i.AppId).ToList();

                var allSavedIds = savedLayout.Select(i => i.AppId).ToHashSet();
                foreach (var app in defaultApps)
                {
                    if (!allSavedIds.Contains(app.Id)) _apps.Add(app);
                }
            }
            else
            {
                _apps = defaultApps;
                _dockApps = new List<AppIconModel>();
            }

            RenderAppGrid();
            UpdateDock();

            _longPressTimer = new DispatcherTimer();
            _longPressTimer.Interval = TimeSpan.FromMilliseconds(500);
            _longPressTimer.Tick += LongPressTimer_Tick;
        }

        private List<AppIconModel> GetDefaultApps()
        {
            return new List<AppIconModel>
            {
                new AppIconModel { Id = "phone", Name = "电话", Icon = "📞", UseImage = false },
                new AppIconModel { Id = "chat", Name = "聊天", Icon = "💬", UseImage = false },
                new AppIconModel { Id = "settings", Name = "设置", Icon = "settings", UseImage = true },
                new AppIconModel { Id = "safari", Name = "Safari", Icon = "🧭", UseImage = false },
                new AppIconModel { Id = "music", Name = "音乐", Icon = "🎵", UseImage = false },
                new AppIconModel { Id = "camera", Name = "相机", Icon = "📷", UseImage = false },
                new AppIconModel { Id = "calendar", Name = "日历", Icon = "📅", UseImage = false },
                new AppIconModel { Id = "wangwang", Name = "汪汪", Icon = "🐶", UseImage = false },
            };
        }

        #endregion

        #region 应用网格渲染

        private void RenderAppGrid()
        {
            AppGridCanvas.Children.Clear();
            _wiggleStoryboards.Clear();

            for (int i = 0; i < _apps.Count; i++)
            {
                var app = _apps[i];
                var panel = CreateAppIconPanel(app, i);
                Canvas.SetLeft(panel, (i % Columns) * CellWidth);
                Canvas.SetTop(panel, (i / Columns) * CellHeight);
                AppGridCanvas.Children.Add(panel);
                if (_isEditMode) StartWiggleAnimation(panel, i);
            }
        }

        private StackPanel CreateAppIconPanel(AppIconModel app, int index)
        {
            var panel = new StackPanel { Width = CellWidth, HorizontalAlignment = HorizontalAlignment.Center, Cursor = Cursors.Hand, Tag = index };
            var iconContainer = new Border { Width = 60, Height = 60, HorizontalAlignment = HorizontalAlignment.Center };

            if (app.UseImage)
            {
                var grid = new Grid();
                try { grid.Children.Add(new Image { Source = new BitmapImage(new Uri("Assets/Setting_Light.png", UriKind.Relative)), Width = 60, Height = 60 }); } catch { }
                iconContainer.Child = grid;
            }
            else
            {
                iconContainer.Child = new TextBlock { Text = app.Icon, FontSize = 48, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center };
            }

            panel.Children.Add(iconContainer);
            panel.Children.Add(new TextBlock { Text = app.Name, Foreground = Brushes.White, FontSize = 12, HorizontalAlignment = HorizontalAlignment.Center, Margin = new Thickness(0, 5, 0, 0) });

            panel.MouseLeftButtonDown += AppIcon_MouseLeftButtonDown;
            panel.MouseLeftButtonUp += AppIcon_MouseLeftButtonUp;
            panel.MouseMove += AppIcon_MouseMove;
            panel.MouseLeave += AppIcon_MouseLeave;

            return panel;
        }

        private void UpdateDock()
        {
            DockPanel.Children.Clear();
            for (int i = 0; i < _dockApps.Count; i++)
                DockPanel.Children.Add(CreateDockIconPanel(_dockApps[i], i));

            if (_isEditMode && _dockApps.Count < MaxDockApps)
            {
                for (int i = _dockApps.Count; i < MaxDockApps; i++)
                {
                    DockPanel.Children.Add(new Border
                    {
                        Width = 55, Height = 55, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center,
                        BorderBrush = new SolidColorBrush(Color.FromArgb(100, 255, 255, 255)), BorderThickness = new Thickness(2),
                        CornerRadius = new CornerRadius(15), Opacity = 0.3,
                        Child = new TextBlock { Text = "+", FontSize = 20, Foreground = new SolidColorBrush(Color.FromArgb(128, 255, 255, 255)), HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center }
                    });
                }
            }
        }

        private StackPanel CreateDockIconPanel(AppIconModel app, int index)
        {
            var panel = new StackPanel { Width = 60, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center, Cursor = Cursors.Hand, Tag = index };
            var container = new Border { Width = 60, Height = 60, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center };

            if (app.UseImage)
            {
                try { container.Child = new Image { Source = new BitmapImage(new Uri("Assets/Setting_Light.png", UriKind.Relative)), Width = 60, Height = 60 }; } catch { }
            }
            else
            {
                container.Child = new TextBlock { Text = app.Icon, FontSize = 48, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center };
            }

            panel.Children.Add(container);
            panel.MouseLeftButtonDown += DockIcon_MouseLeftButtonDown;
            panel.MouseLeftButtonUp += DockIcon_MouseLeftButtonUp;
            panel.MouseMove += DockIcon_MouseMove;
            panel.MouseLeave += DockIcon_MouseLeave;
            return panel;
        }

        #endregion

        #region 应用图标拖拽事件

        private void AppIcon_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            if (sender is StackPanel panel)
            {
                _longPressTarget = panel;
                _longPressStartPos = e.GetPosition(AppGridCanvas);
                _dragSource = "grid";
                _longPressTimer.Start();
                if (_isEditMode)
                {
                    _dragStartPoint = e.GetPosition(AppGridCanvas);
                    _draggedIndex = (int)panel.Tag;
                    _dragSource = "grid";
                }
            }
        }

        private void AppIcon_MouseLeftButtonUp(object sender, MouseButtonEventArgs e)
        {
            _longPressTimer.Stop();
            if (!_isEditMode && !_isDragging)
            {
                if (sender is StackPanel panel)
                {
                    int index = (int)panel.Tag;
                    if (index >= 0 && index < _apps.Count)
                    {
                        if (_apps[index].Id == "settings") OnSettingsClick(sender, null);
                        else if (_apps[index].Id == "chat") OnChatClick();
                    }
                }
            }
            if (_isDragging)
            {
                var mousePos = e.GetPosition(this);
                if (_dragSource == "grid" && IsPointerInDock(mousePos) && _dockApps.Count < MaxDockApps)
                {
                    var app = _apps[_draggedIndex];
                    _apps.RemoveAt(_draggedIndex);
                    _dockApps.Add(app);
                }
                FinishDrag();
            }
            _longPressTarget = null;
        }

        private void AppIcon_MouseMove(object sender, MouseEventArgs e)
        {
            if (_longPressTimer.IsEnabled)
            {
                var currentPos = e.GetPosition(AppGridCanvas);
                if (Math.Abs(currentPos.X - _longPressStartPos.X) > 5 || Math.Abs(currentPos.Y - _longPressStartPos.Y) > 5)
                    _longPressTimer.Stop();
            }

            if (_isEditMode && e.LeftButton == MouseButtonState.Pressed && _draggedIndex >= 0 && _dragSource == "grid")
            {
                var currentPos = e.GetPosition(AppGridCanvas);
                var diff = currentPos - _dragStartPoint;

                if (!_isDragging && (Math.Abs(diff.X) > 3 || Math.Abs(diff.Y) > 3))
                {
                    _isDragging = true;
                    _draggedElement = sender as StackPanel;
                    if (_draggedElement != null)
                    {
                        _draggedElement.Opacity = 0.3;
                        CreateDragOverlay(_draggedElement);
                    }
                }

                if (_isDragging && _dragOverlayElement != null)
                {
                    var windowPos = e.GetPosition(RootGrid);
                    Canvas.SetLeft(_dragOverlayElement, windowPos.X - CellWidth / 2);
                    Canvas.SetTop(_dragOverlayElement, windowPos.Y - CellHeight / 2);

                    var mousePos = e.GetPosition(this);
                    HighlightDockIfNeeded(mousePos);

                    int targetCol = Math.Max(0, Math.Min(Columns - 1, (int)(currentPos.X / CellWidth)));
                    int targetRow = Math.Max(0, (int)(currentPos.Y / CellHeight));
                    int targetIndex = Math.Min(_apps.Count - 1, Math.Max(0, targetRow * Columns + targetCol));

                    if (targetIndex != _draggedIndex && !IsPointerInDock(mousePos))
                    {
                        var draggedApp = _apps[_draggedIndex];
                        _apps.RemoveAt(_draggedIndex);
                        _apps.Insert(targetIndex, draggedApp);
                        _dragStartPoint = currentPos;
                        _draggedIndex = targetIndex;
                        RenderAppGridExcept(targetIndex);
                    }
                }
            }
        }

        private void AppIcon_MouseLeave(object sender, MouseEventArgs e) { _longPressTimer.Stop(); }

        #endregion

        #region Dock栏图标事件

        private void DockIcon_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            if (sender is StackPanel panel)
            {
                _longPressTarget = panel;
                _longPressStartPos = e.GetPosition(this);
                _dragSource = "dock";
                _longPressTimer.Start();
                if (_isEditMode)
                {
                    _dragStartPoint = e.GetPosition(this);
                    _draggedIndex = (int)panel.Tag;
                    _dragSource = "dock";
                }
            }
        }

        private void DockIcon_MouseLeftButtonUp(object sender, MouseButtonEventArgs e)
        {
            _longPressTimer.Stop();
            if (!_isEditMode && !_isDragging)
            {
                if (sender is StackPanel panel)
                {
                    int index = (int)panel.Tag;
                    if (index >= 0 && index < _dockApps.Count)
                    {
                        if (_dockApps[index].Id == "settings") OnSettingsClick(sender, null);
                        else if (_dockApps[index].Id == "chat") OnChatClick();
                    }
                }
            }
            if (_isDragging && _dragSource == "dock")
            {
                var mousePos = e.GetPosition(this);
                if (!IsPointerInDock(mousePos))
                {
                    var app = _dockApps[_draggedIndex];
                    _dockApps.RemoveAt(_draggedIndex);
                    _apps.Add(app);
                }
                FinishDrag();
            }
            _longPressTarget = null;
        }

        private void DockIcon_MouseMove(object sender, MouseEventArgs e)
        {
            if (_longPressTimer.IsEnabled)
            {
                var currentPos = e.GetPosition(this);
                if (Math.Abs(currentPos.X - _longPressStartPos.X) > 5 || Math.Abs(currentPos.Y - _longPressStartPos.Y) > 5)
                    _longPressTimer.Stop();
            }

            if (_isEditMode && e.LeftButton == MouseButtonState.Pressed && _draggedIndex >= 0 && _dragSource == "dock")
            {
                var currentPos = e.GetPosition(this);
                var diff = currentPos - _dragStartPoint;

                if (!_isDragging && (Math.Abs(diff.X) > 3 || Math.Abs(diff.Y) > 3))
                {
                    _isDragging = true;
                    _draggedElement = sender as StackPanel;
                    if (_draggedElement != null)
                    {
                        _draggedElement.Opacity = 0.3;
                        CreateDragOverlay(_draggedElement);
                    }
                }

                if (_isDragging && _dragOverlayElement != null)
                {
                    var windowPos = e.GetPosition(RootGrid);
                    Canvas.SetLeft(_dragOverlayElement, windowPos.X - 30);
                    Canvas.SetTop(_dragOverlayElement, windowPos.Y - 30);

                    if (DockPanel.Children.Count > 0 && _dockApps.Count > 1)
                    {
                        double dockWidth = DockPanel.ActualWidth;
                        double cellW = dockWidth / _dockApps.Count;
                        double relX = currentPos.X - DockPanel.TranslatePoint(new Point(0, 0), this).X;
                        int targetIdx = Math.Max(0, Math.Min(_dockApps.Count - 1, (int)(relX / cellW)));
                        if (targetIdx != _draggedIndex)
                        {
                            var draggedApp = _dockApps[_draggedIndex];
                            _dockApps.RemoveAt(_draggedIndex);
                            _dockApps.Insert(targetIdx, draggedApp);
                            _dragStartPoint = currentPos;
                            _draggedIndex = targetIdx;
                            UpdateDock();
                        }
                    }
                }
            }
        }

        private void DockIcon_MouseLeave(object sender, MouseEventArgs e) { _longPressTimer.Stop(); }

        #endregion

        #region Dock区域判断与高亮

        private bool IsPointerInDock(Point windowPos)
        {
            try
            {
                var dockBorder = DockBorder;
                if (dockBorder == null) return false;
                var dockPos = dockBorder.TranslatePoint(new Point(0, 0), this);
                return windowPos.Y >= dockPos.Y && windowPos.Y <= dockPos.Y + dockBorder.ActualHeight &&
                       windowPos.X >= dockPos.X && windowPos.X <= dockPos.X + dockBorder.ActualWidth;
            }
            catch { return false; }
        }

        private void HighlightDockIfNeeded(Point windowPos)
        {
            if (DockBorder == null) return;
            if (IsPointerInDock(windowPos) && _dragSource == "grid" && _dockApps.Count < MaxDockApps)
            {
                DockBorder.BorderBrush = new SolidColorBrush(Color.FromArgb(150, 0, 122, 255));
                DockBorder.BorderThickness = new Thickness(2);
            }
            else
            {
                DockBorder.BorderBrush = null;
                DockBorder.BorderThickness = new Thickness(0);
            }
        }

        private void LongPressTimer_Tick(object sender, EventArgs e)
        {
            _longPressTimer.Stop();
            if (!_isEditMode)
            {
                EnterEditMode();
                if (_longPressTarget != null)
                {
                    _draggedIndex = (int)_longPressTarget.Tag;
                    _dragStartPoint = _longPressStartPos;
                }
            }
        }

        #endregion

        #region 编辑模式

        private void EnterEditMode()
        {
            _isEditMode = true;
            EditModeBanner.Visibility = Visibility.Visible;
            RenderAppGrid();
            UpdateDock();
        }

        private void ExitEditMode()
        {
            _isEditMode = false;
            EditModeBanner.Visibility = Visibility.Collapsed;
            foreach (var sb in _wiggleStoryboards) sb.Stop();
            _wiggleStoryboards.Clear();
            if (DockBorder != null) { DockBorder.BorderBrush = null; DockBorder.BorderThickness = new Thickness(0); }
            RenderAppGrid();
            UpdateDock();
            SaveCurrentLayout();
        }

        private void OnEditDoneClick(object sender, MouseButtonEventArgs e) { ExitEditMode(); }

        private void StartWiggleAnimation(StackPanel panel, int index)
        {
            var rotateTransform = new RotateTransform(0, CellWidth / 2, 10);
            panel.RenderTransform = rotateTransform;
            var animation = new DoubleAnimation
            {
                From = index % 2 == 0 ? -1.5 : 1.5, To = index % 2 == 0 ? 1.5 : -1.5,
                Duration = TimeSpan.FromMilliseconds(120 + (index % 3) * 30),
                AutoReverse = true, RepeatBehavior = RepeatBehavior.Forever, EasingFunction = new SineEase()
            };
            var storyboard = new Storyboard();
            Storyboard.SetTarget(animation, panel);
            Storyboard.SetTargetProperty(animation, new PropertyPath("(UIElement.RenderTransform).(RotateTransform.Angle)"));
            storyboard.Children.Add(animation);
            storyboard.Begin();
            _wiggleStoryboards.Add(storyboard);
        }

        private void RenderAppGridExcept(int exceptIndex)
        {
            var tempDraggedIdx = _draggedIndex;
            var tempIsDragging = _isDragging;
            AppGridCanvas.Children.Clear();
            _wiggleStoryboards.Clear();

            for (int i = 0; i < _apps.Count; i++)
            {
                var panel = CreateAppIconPanel(_apps[i], i);
                Canvas.SetLeft(panel, (i % Columns) * CellWidth);
                Canvas.SetTop(panel, (i / Columns) * CellHeight);

                if (i == tempDraggedIdx && tempIsDragging)
                {
                    panel.Opacity = 0.3;
                    _draggedElement = panel;
                }
                else if (_isEditMode) StartWiggleAnimation(panel, i);

                AppGridCanvas.Children.Add(panel);
            }
        }

        #endregion

        #region 拖拽浮层

        private void CreateDragOverlay(FrameworkElement source)
        {
            RemoveDragOverlay();
            // 确保拖拽浮层在最顶层（Panel.ZIndex=99999在XAML中已设置）
            var overlayPanel = new StackPanel
            {
                Opacity = 0.85,
                RenderTransform = new ScaleTransform(1.15, 1.15),
                RenderTransformOrigin = new Point(0.5, 0.5)
            };

            if (source is StackPanel srcPanel && srcPanel.Tag is int idx)
            {
                var appList = _dragSource == "dock" ? _dockApps : _apps;
                if (idx >= 0 && idx < appList.Count)
                {
                    var app = appList[idx];
                    var iconContainer = new Border { Width = 60, Height = 60, HorizontalAlignment = HorizontalAlignment.Center };

                    if (app.UseImage)
                    {
                        try { iconContainer.Child = new Image { Source = new BitmapImage(new Uri("Assets/Setting_Light.png", UriKind.Relative)), Width = 60, Height = 60 }; } catch { }
                    }
                    else
                    {
                        iconContainer.Child = new TextBlock { Text = app.Icon, FontSize = 48, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center };
                    }
                    overlayPanel.Children.Add(iconContainer);

                    if (_dragSource != "dock")
                    {
                        overlayPanel.Children.Add(new TextBlock { Text = app.Name, Foreground = Brushes.White, FontSize = 12, HorizontalAlignment = HorizontalAlignment.Center, Margin = new Thickness(0, 5, 0, 0) });
                    }
                }
            }

            _dragOverlayElement = overlayPanel;
            DragOverlayCanvas.Children.Add(_dragOverlayElement);
            DragOverlayCanvas.Visibility = Visibility.Visible;

            var pos = source.TranslatePoint(new Point(0, 0), RootGrid);
            Canvas.SetLeft(_dragOverlayElement, pos.X);
            Canvas.SetTop(_dragOverlayElement, pos.Y);
        }

        private void RemoveDragOverlay()
        {
            if (_dragOverlayElement != null)
            {
                DragOverlayCanvas.Children.Remove(_dragOverlayElement);
                _dragOverlayElement = null;
            }
            DragOverlayCanvas.Visibility = Visibility.Collapsed;
        }

        #endregion

        #region 拖拽完成

        private void FinishDrag()
        {
            _isDragging = false;
            _draggedIndex = -1;
            RemoveDragOverlay();

            if (_draggedElement != null)
            {
                _draggedElement.Opacity = 1;
                _draggedElement.RenderTransform = null;
                Panel.SetZIndex(_draggedElement, 0);
                _draggedElement = null;
            }

            RenderAppGrid();
            UpdateDock();
            SaveCurrentLayout();
        }

        private void AppGridCanvas_DragOver(object sender, DragEventArgs e) { e.Effects = DragDropEffects.Move; e.Handled = true; }
        private void AppGridCanvas_Drop(object sender, DragEventArgs e) { e.Handled = true; }

        #endregion

        #region 小组件拖拽

        private void ApplyWidgetOrder()
        {
            if (_widgetOrder.Count < 2) return;
            if (_widgetOrder[0] == "clock")
            {
                Grid.SetColumn(ClockWidgetBorder, 0);
                Grid.SetColumn(WeatherWidgetBorder, 1);
                ClockWidgetBorder.Margin = new Thickness(0, 0, 5, 0);
                WeatherWidgetBorder.Margin = new Thickness(5, 0, 0, 0);
            }
            else
            {
                Grid.SetColumn(ClockWidgetBorder, 1);
                Grid.SetColumn(WeatherWidgetBorder, 0);
                ClockWidgetBorder.Margin = new Thickness(5, 0, 0, 0);
                WeatherWidgetBorder.Margin = new Thickness(0, 0, 5, 0);
            }
        }

        private void Widget_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            if (sender is Border widget)
            {
                _draggedWidget = widget;
                _widgetDragStartPos = e.GetPosition(this);
                _widgetLongPressTimer.Interval = _isEditMode ? TimeSpan.FromMilliseconds(150) : TimeSpan.FromMilliseconds(500);
                _widgetLongPressTimer.Start();
            }
        }

        private void Widget_MouseLeftButtonUp(object sender, MouseButtonEventArgs e)
        {
            _widgetLongPressTimer.Stop();
            if (_isWidgetDragging)
            {
                var mousePos = e.GetPosition(this);
                var otherWidget = GetOtherWidget(_draggedWidget);
                if (otherWidget != null)
                {
                    var otherPos = otherWidget.TranslatePoint(new Point(0, 0), this);
                    var otherRect = new Rect(otherPos, new Size(otherWidget.ActualWidth, otherWidget.ActualHeight));
                    if (otherRect.Contains(mousePos))
                    {
                        var temp = _widgetOrder[0];
                        _widgetOrder[0] = _widgetOrder[1];
                        _widgetOrder[1] = temp;
                        ApplyWidgetOrder();
                        SaveCurrentLayout();
                    }
                }

                RemoveDragOverlay();
                if (_draggedWidget != null) { _draggedWidget.Opacity = 1; _draggedWidget.RenderTransform = null; }
                if (otherWidget != null) { otherWidget.Opacity = 1; otherWidget.RenderTransform = null; }
                _isWidgetDragging = false;
                _draggedWidget = null;
            }
        }

        private void Widget_MouseMove(object sender, MouseEventArgs e)
        {
            if (_widgetLongPressTimer.IsEnabled)
            {
                var currentPos = e.GetPosition(this);
                if (Math.Abs(currentPos.X - _widgetDragStartPos.X) > 5 || Math.Abs(currentPos.Y - _widgetDragStartPos.Y) > 5)
                    _widgetLongPressTimer.Stop();
            }

            if (_isWidgetDragging && _draggedWidget != null && _dragOverlayElement != null)
            {
                var windowPos = e.GetPosition(RootGrid);
                Canvas.SetLeft(_dragOverlayElement, windowPos.X - _draggedWidget.ActualWidth / 2);
                Canvas.SetTop(_dragOverlayElement, windowPos.Y - _draggedWidget.ActualHeight / 2);

                var otherWidget = GetOtherWidget(_draggedWidget);
                if (otherWidget != null)
                {
                    var mousePos = e.GetPosition(this);
                    var otherPos = otherWidget.TranslatePoint(new Point(0, 0), this);
                    var otherRect = new Rect(otherPos, new Size(otherWidget.ActualWidth, otherWidget.ActualHeight));
                    if (otherRect.Contains(mousePos))
                    {
                        otherWidget.Opacity = 0.6;
                        otherWidget.RenderTransform = new ScaleTransform(0.92, 0.92, otherWidget.ActualWidth / 2, otherWidget.ActualHeight / 2);
                    }
                    else
                    {
                        otherWidget.Opacity = 1;
                        otherWidget.RenderTransform = null;
                    }
                }
            }
        }

        private void Widget_MouseLeave(object sender, MouseEventArgs e)
        {
            _widgetLongPressTimer.Stop();
        }

        private void WidgetLongPressTimer_Tick(object sender, EventArgs e)
        {
            _widgetLongPressTimer.Stop();
            if (!_isEditMode) EnterEditMode();

            if (_draggedWidget != null)
            {
                _isWidgetDragging = true;
                _draggedWidget.Opacity = 0.3;

                // 创建小组件浮层
                RemoveDragOverlay();
                var overlayBorder = new Border
                {
                    Width = _draggedWidget.ActualWidth,
                    Height = _draggedWidget.ActualHeight,
                    CornerRadius = new CornerRadius(20),
                    Opacity = 0.85,
                    RenderTransform = new ScaleTransform(1.05, 1.05),
                    RenderTransformOrigin = new Point(0.5, 0.5)
                };

                if (_draggedWidget == ClockWidgetBorder)
                {
                    overlayBorder.Background = new LinearGradientBrush(Color.FromRgb(0xE0, 0xC3, 0xFC), Color.FromRgb(0x8E, 0xC5, 0xFC), 45);
                    overlayBorder.Child = new TextBlock { Text = "🕐", FontSize = 40, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center, Foreground = Brushes.White };
                }
                else
                {
                    overlayBorder.Background = new LinearGradientBrush(Color.FromRgb(0x4F, 0xAC, 0xFE), Color.FromRgb(0x00, 0xF2, 0xFE), 45);
                    overlayBorder.Child = new TextBlock { Text = "🌤️", FontSize = 40, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center, Foreground = Brushes.White };
                }

                _dragOverlayElement = overlayBorder;
                DragOverlayCanvas.Children.Add(_dragOverlayElement);
                DragOverlayCanvas.Visibility = Visibility.Visible;

                var pos = _draggedWidget.TranslatePoint(new Point(0, 0), RootGrid);
                Canvas.SetLeft(_dragOverlayElement, pos.X);
                Canvas.SetTop(_dragOverlayElement, pos.Y);
            }
        }

        private Border GetOtherWidget(Border widget)
        {
            if (widget == ClockWidgetBorder) return WeatherWidgetBorder;
            if (widget == WeatherWidgetBorder) return ClockWidgetBorder;
            return null;
        }

        #endregion

        #region 布局持久化

        private void SaveCurrentLayout()
        {
            var items = new List<Core.LayoutItem>();
            for (int i = 0; i < _apps.Count; i++)
                items.Add(new Core.LayoutItem { AppId = _apps[i].Id, Position = i, Area = "grid" });
            for (int i = 0; i < _dockApps.Count; i++)
                items.Add(new Core.LayoutItem { AppId = _dockApps[i].Id, Position = i, Area = "dock" });
            for (int i = 0; i < _widgetOrder.Count; i++)
                items.Add(new Core.LayoutItem { AppId = _widgetOrder[i], Position = i, Area = "widget" });
            _layoutManager.SaveLayout(items);
        }

        #endregion

        #region 时钟和天气

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
            await Task.Delay(500);
            string city = "广州";
            ClockCityText.Text = city;
            WeatherCityText.Text = city;
            WeatherTempText.Text = "25°";
            WeatherIconText.Text = "⛅";
            WeatherDescText.Text = "多云";
            WeatherRangeText.Text = "H:29° L:21°";
        }

        #endregion

        #region 聊天App

        private void OnChatClick()
        {
            ChatAppOverlay.Visibility = Visibility.Visible;
        }

        #endregion

        #region 设置与激活

        private void OnSettingsClick(object sender, MouseButtonEventArgs e)
        {
            SettingsOverlay.Visibility = Visibility.Visible;
        }

        private void OnBackClick(object sender, RoutedEventArgs e)
        {
            SettingsOverlay.Visibility = Visibility.Collapsed;
        }

        private void OnDisplaySettingsClick(object sender, RoutedEventArgs e)
        {
            DisplaySettingsOverlay.Visibility = Visibility.Visible;
            UpdateWallpaperUI();
        }

        private void OnDisplaySettingsBackClick(object sender, RoutedEventArgs e)
        {
            DisplaySettingsOverlay.Visibility = Visibility.Collapsed;
        }

        private void OnSelectLockWallpaperClick(object sender, RoutedEventArgs e) { SelectWallpaper("lock"); }
        private void OnSelectHomeWallpaperClick(object sender, RoutedEventArgs e) { SelectWallpaper("home"); }

        private void SelectWallpaper(string type)
        {
            var dialog = new Microsoft.Win32.OpenFileDialog();
            dialog.Filter = "Image files (*.png;*.jpeg;*.jpg)|*.png;*.jpeg;*.jpg|All files (*.*)|*.*";
            if (dialog.ShowDialog() == true)
            {
                string fileName = WallpaperManager.Instance.CopyImageToStorage(dialog.FileName);
                if (fileName != null)
                {
                    WallpaperManager.Instance.SaveWallpaper(type, fileName);
                    UpdateWallpaperUI();
                    ApplyWallpapers();
                }
            }
        }

        private void UpdateWallpaperUI()
        {
            string lockPath = WallpaperManager.Instance.GetWallpaperFilePath("lock");
            if (lockPath != null)
            {
                LockWpStatusText.Text = "已设置，点击更换";
                LockWpPreview.Child = new Image { Source = new BitmapImage(new Uri(lockPath)), Stretch = Stretch.UniformToFill };
            }
            string homePath = WallpaperManager.Instance.GetWallpaperFilePath("home");
            if (homePath != null)
            {
                HomeWpStatusText.Text = "已设置，点击更换";
                HomeWpPreview.Child = new Image { Source = new BitmapImage(new Uri(homePath)), Stretch = Stretch.UniformToFill };
            }
        }

        private void ApplyWallpapers()
        {
            string homePath = WallpaperManager.Instance.GetWallpaperFilePath("home");
            if (homePath != null)
                this.Background = new ImageBrush(new BitmapImage(new Uri(homePath))) { Stretch = Stretch.UniformToFill };
        }

        private void OnActivationClickUI(object sender, RoutedEventArgs e) { ActivationOverlay.Visibility = Visibility.Visible; }
        private void OnActivationBackClick(object sender, RoutedEventArgs e) { ActivationOverlay.Visibility = Visibility.Collapsed; }
        private void OnCopyMachineId(object sender, RoutedEventArgs e) { Clipboard.SetText(MachineIdTextBox.Text); }
        private void OnPasteLicenseKey(object sender, RoutedEventArgs e) { if (Clipboard.ContainsText()) LicenseKeyTextBox.Text = Clipboard.GetText(); }

        protected override void OnKeyDown(KeyEventArgs e)
        {
            if (e.Key == Key.Escape)
            {
                if (_isEditMode) { ExitEditMode(); e.Handled = true; }
                else if (ActivationOverlay.Visibility == Visibility.Visible) { ActivationOverlay.Visibility = Visibility.Collapsed; e.Handled = true; }
                else if (ChatAppOverlay.Visibility == Visibility.Visible) { ChatAppOverlay.Visibility = Visibility.Collapsed; e.Handled = true; }
                else if (SettingsOverlay.Visibility == Visibility.Visible) { SettingsOverlay.Visibility = Visibility.Collapsed; e.Handled = true; }
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

        #endregion
    }
}