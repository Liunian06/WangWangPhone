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
    /// <summary>
    /// 应用图标数据模型
    /// </summary>
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

        // 主屏幕应用图标列表（可重排序）
        private List<AppIconModel> _apps = new List<AppIconModel>();

        // Dock栏应用列表（独立管理，初始为空，最多4个）
        private List<AppIconModel> _dockApps = new List<AppIconModel>();
        private const int MaxDockApps = 4;

        // 编辑模式相关
        private bool _isEditMode = false;
        private DispatcherTimer _longPressTimer;
        private StackPanel _longPressTarget;
        private Point _longPressStartPos;

        // 拖拽相关
        private bool _isDragging = false;
        private StackPanel _draggedElement;
        private int _draggedIndex = -1;
        private Point _dragStartPoint;
        private string _dragSource = "grid"; // "grid" 或 "dock"

        // 网格布局参数
        private const int Columns = 4;
        private const double CellWidth = 85;
        private const double CellHeight = 105;

        // 抖动动画
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

            // Wire up chat app close event
            ChatAppControl.OnCloseRequested += () =>
            {
                ChatAppOverlay.Visibility = Visibility.Collapsed;
            };
        }

        #region 初始化

        private void ApplyThemeIcons()
        {
            bool isDark = SystemParameters.HighContrast;
            // Default to Light for prototype
        }

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

        /// <summary>
        /// 初始化布局管理器并加载保存的布局
        /// </summary>
        private void InitializeLayout()
        {
            _layoutManager.Initialize();

            // 默认应用列表
            var defaultApps = GetDefaultApps();

            // 从数据库加载布局
            var savedLayout = _layoutManager.GetLayout();
            if (savedLayout.Count > 0)
            {
                // 加载主屏幕网格
                var gridItems = savedLayout.Where(i => i.Area == "grid").OrderBy(i => i.Position).ToList();
                _apps.Clear();
                foreach (var layoutItem in gridItems)
                {
                    var app = defaultApps.FirstOrDefault(a => a.Id == layoutItem.AppId);
                    if (app != null)
                    {
                        _apps.Add(app);
                    }
                }

                // 加载Dock栏
                var dockItems = savedLayout.Where(i => i.Area == "dock").OrderBy(i => i.Position).ToList();
                _dockApps.Clear();
                foreach (var layoutItem in dockItems)
                {
                    var app = defaultApps.FirstOrDefault(a => a.Id == layoutItem.AppId);
                    if (app != null)
                    {
                        _dockApps.Add(app);
                    }
                }

                // 补充数据库中没有的新应用到主屏幕（排除已在dock中的）
                var allSavedIds = savedLayout.Select(i => i.AppId).ToHashSet();
                foreach (var app in defaultApps)
                {
                    if (!allSavedIds.Contains(app.Id))
                    {
                        _apps.Add(app);
                    }
                }
            }
            else
            {
                _apps = defaultApps;
                _dockApps = new List<AppIconModel>(); // Dock初始为空
            }

            RenderAppGrid();
            UpdateDock();

            // 初始化长按计时器
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

        /// <summary>
        /// 在 Canvas 上渲染所有应用图标
        /// </summary>
        private void RenderAppGrid()
        {
            AppGridCanvas.Children.Clear();
            _wiggleStoryboards.Clear();

            for (int i = 0; i < _apps.Count; i++)
            {
                var app = _apps[i];
                var panel = CreateAppIconPanel(app, i);

                int row = i / Columns;
                int col = i % Columns;

                Canvas.SetLeft(panel, col * CellWidth);
                Canvas.SetTop(panel, row * CellHeight);

                AppGridCanvas.Children.Add(panel);

                // 编辑模式抖动动画
                if (_isEditMode)
                {
                    StartWiggleAnimation(panel, i);
                }
            }
        }

        /// <summary>
        /// 创建单个应用图标面板
        /// </summary>
        private StackPanel CreateAppIconPanel(AppIconModel app, int index)
        {
            var panel = new StackPanel
            {
                Width = CellWidth,
                HorizontalAlignment = HorizontalAlignment.Center,
                Cursor = Cursors.Hand,
                Tag = index // 存储索引
            };

            var iconContainer = new Border
            {
                Width = 60,
                Height = 60,
                HorizontalAlignment = HorizontalAlignment.Center
            };

            if (app.UseImage)
            {
                var grid = new Grid();
                try
                {
                    var lightImage = new Image
                    {
                        Source = new BitmapImage(new Uri("Assets/Setting_Light.png", UriKind.Relative)),
                        Width = 60,
                        Height = 60
                    };
                    grid.Children.Add(lightImage);
                }
                catch { }
                iconContainer.Child = grid;
            }
            else
            {
                iconContainer.Child = new TextBlock
                {
                    Text = app.Icon,
                    FontSize = 48,
                    HorizontalAlignment = HorizontalAlignment.Center,
                    VerticalAlignment = VerticalAlignment.Center
                };
            }

            var nameBlock = new TextBlock
            {
                Text = app.Name,
                Foreground = Brushes.White,
                FontSize = 12,
                HorizontalAlignment = HorizontalAlignment.Center,
                Margin = new Thickness(0, 5, 0, 0)
            };

            panel.Children.Add(iconContainer);
            panel.Children.Add(nameBlock);

            // 事件绑定
            panel.MouseLeftButtonDown += AppIcon_MouseLeftButtonDown;
            panel.MouseLeftButtonUp += AppIcon_MouseLeftButtonUp;
            panel.MouseMove += AppIcon_MouseMove;
            panel.MouseLeave += AppIcon_MouseLeave;

            return panel;
        }

        /// <summary>
        /// 更新 Dock 栏（独立管理，不再自动取前4个）
        /// </summary>
        private void UpdateDock()
        {
            DockPanel.Children.Clear();

            // 渲染Dock栏中的应用
            for (int i = 0; i < _dockApps.Count; i++)
            {
                var app = _dockApps[i];
                var panel = CreateDockIconPanel(app, i);
                DockPanel.Children.Add(panel);
            }

            // 编辑模式下，如果Dock未满，显示占位符
            if (_isEditMode && _dockApps.Count < MaxDockApps)
            {
                for (int i = _dockApps.Count; i < MaxDockApps; i++)
                {
                    var placeholder = new Border
                    {
                        Width = 55,
                        Height = 55,
                        HorizontalAlignment = HorizontalAlignment.Center,
                        VerticalAlignment = VerticalAlignment.Center,
                        BorderBrush = new SolidColorBrush(Color.FromArgb(100, 255, 255, 255)),
                        BorderThickness = new Thickness(2),
                        CornerRadius = new CornerRadius(15),
                        Opacity = 0.3,
                        Child = new TextBlock
                        {
                            Text = "+",
                            FontSize = 20,
                            Foreground = new SolidColorBrush(Color.FromArgb(128, 255, 255, 255)),
                            HorizontalAlignment = HorizontalAlignment.Center,
                            VerticalAlignment = VerticalAlignment.Center
                        }
                    };
                    DockPanel.Children.Add(placeholder);
                }
            }
        }

        /// <summary>
        /// 创建Dock栏图标面板
        /// </summary>
        private StackPanel CreateDockIconPanel(AppIconModel app, int index)
        {
            var panel = new StackPanel
            {
                Width = 60,
                HorizontalAlignment = HorizontalAlignment.Center,
                VerticalAlignment = VerticalAlignment.Center,
                Cursor = Cursors.Hand,
                Tag = index
            };

            var container = new Border
            {
                Width = 60,
                Height = 60,
                HorizontalAlignment = HorizontalAlignment.Center,
                VerticalAlignment = VerticalAlignment.Center
            };

            if (app.UseImage)
            {
                try
                {
                    container.Child = new Image
                    {
                        Source = new BitmapImage(new Uri("Assets/Setting_Light.png", UriKind.Relative)),
                        Width = 60,
                        Height = 60
                    };
                }
                catch { }
            }
            else
            {
                container.Child = new TextBlock
                {
                    Text = app.Icon,
                    FontSize = 48,
                    HorizontalAlignment = HorizontalAlignment.Center,
                    VerticalAlignment = VerticalAlignment.Center
                };
            }

            panel.Children.Add(container);

            // Dock栏图标事件绑定
            panel.MouseLeftButtonDown += DockIcon_MouseLeftButtonDown;
            panel.MouseLeftButtonUp += DockIcon_MouseLeftButtonUp;
            panel.MouseMove += DockIcon_MouseMove;
            panel.MouseLeave += DockIcon_MouseLeave;

            return panel;
        }

        #endregion

        #region 长按进入编辑模式

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
                    // 编辑模式下直接开始拖拽准备
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
                // 普通点击
                if (sender is StackPanel panel)
                {
                    int index = (int)panel.Tag;
                    if (index >= 0 && index < _apps.Count)
                    {
                        if (_apps[index].Id == "settings")
                            OnSettingsClick(sender, null);
                        else if (_apps[index].Id == "chat")
                            OnChatClick();
                    }
                }
            }

            if (_isDragging)
            {
                // 检查是否拖到了Dock栏区域
                var mousePos = e.GetPosition(this);
                if (_dragSource == "grid" && IsPointerInDock(mousePos) && _dockApps.Count < MaxDockApps)
                {
                    // 从主屏幕拖到Dock栏
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
                // 检查移动距离是否超过阈值，超过则取消长按
                var currentPos = e.GetPosition(AppGridCanvas);
                if (Math.Abs(currentPos.X - _longPressStartPos.X) > 5 ||
                    Math.Abs(currentPos.Y - _longPressStartPos.Y) > 5)
                {
                    _longPressTimer.Stop();
                }
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
                        _draggedElement.Opacity = 0.7;
                        Panel.SetZIndex(_draggedElement, 100);
                        _draggedElement.RenderTransform = new ScaleTransform(1.15, 1.15, CellWidth / 2, CellHeight / 2);
                    }
                }

                if (_isDragging && _draggedElement != null)
                {
                    // 移动被拖拽的元素
                    int origRow = _draggedIndex / Columns;
                    int origCol = _draggedIndex % Columns;
                    Canvas.SetLeft(_draggedElement, origCol * CellWidth + diff.X);
                    Canvas.SetTop(_draggedElement, origRow * CellHeight + diff.Y);

                    // 检查是否在Dock区域上方（高亮提示）
                    var mousePos = e.GetPosition(this);
                    HighlightDockIfNeeded(mousePos);

                    // 计算目标位置（仅在grid区域内排序）
                    double centerX = origCol * CellWidth + CellWidth / 2 + diff.X;
                    double centerY = origRow * CellHeight + CellHeight / 2 + diff.Y;

                    int targetCol = Math.Max(0, Math.Min(Columns - 1, (int)(centerX / CellWidth)));
                    int targetRow = Math.Max(0, (int)(centerY / CellHeight));
                    int targetIndex = Math.Min(_apps.Count - 1, Math.Max(0, targetRow * Columns + targetCol));

                    if (targetIndex != _draggedIndex && !IsPointerInDock(mousePos))
                    {
                        // 交换位置
                        var draggedApp = _apps[_draggedIndex];
                        _apps.RemoveAt(_draggedIndex);
                        _apps.Insert(targetIndex, draggedApp);

                        // 更新拖拽参考点
                        _dragStartPoint = currentPos;
                        _draggedIndex = targetIndex;

                        // 重新渲染其他图标（不包括被拖拽的）
                        RenderAppGridExcept(targetIndex);
                    }
                }
            }
        }

        private void AppIcon_MouseLeave(object sender, MouseEventArgs e)
        {
            _longPressTimer.Stop();
        }

        // ============================================
        // Dock栏图标事件
        // ============================================
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
                // Dock栏普通点击
                if (sender is StackPanel panel)
                {
                    int index = (int)panel.Tag;
                    if (index >= 0 && index < _dockApps.Count)
                    {
                        if (_dockApps[index].Id == "settings")
                            OnSettingsClick(sender, null);
                        else if (_dockApps[index].Id == "chat")
                            OnChatClick();
                    }
                }
            }

            if (_isDragging && _dragSource == "dock")
            {
                // 检查是否拖到了主屏幕区域
                var mousePos = e.GetPosition(this);
                if (!IsPointerInDock(mousePos))
                {
                    // 从Dock拖回主屏幕
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
                if (Math.Abs(currentPos.X - _longPressStartPos.X) > 5 ||
                    Math.Abs(currentPos.Y - _longPressStartPos.Y) > 5)
                {
                    _longPressTimer.Stop();
                }
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
                        _draggedElement.Opacity = 0.7;
                        Panel.SetZIndex(_draggedElement, 100);
                        _draggedElement.RenderTransform = new ScaleTransform(1.15, 1.15, 27.5, 27.5);
                    }
                }

                if (_isDragging && _draggedElement != null)
                {
                    // Dock内排序
                    var dockPanel = DockPanel;
                    if (dockPanel.Children.Count > 0 && _dockApps.Count > 1)
                    {
                        double dockWidth = dockPanel.ActualWidth;
                        double cellW = dockWidth / _dockApps.Count;
                        double relX = currentPos.X - dockPanel.TranslatePoint(new Point(0, 0), this).X;
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

        private void DockIcon_MouseLeave(object sender, MouseEventArgs e)
        {
            _longPressTimer.Stop();
        }

        /// <summary>
        /// 判断鼠标位置是否在Dock栏区域内
        /// </summary>
        private bool IsPointerInDock(Point windowPos)
        {
            try
            {
                var dockBorder = DockPanel.Parent as Border;
                if (dockBorder == null) return false;
                var dockPos = dockBorder.TranslatePoint(new Point(0, 0), this);
                return windowPos.Y >= dockPos.Y &&
                       windowPos.Y <= dockPos.Y + dockBorder.ActualHeight &&
                       windowPos.X >= dockPos.X &&
                       windowPos.X <= dockPos.X + dockBorder.ActualWidth;
            }
            catch
            {
                return false;
            }
        }

        /// <summary>
        /// 拖拽时高亮Dock栏
        /// </summary>
        private void HighlightDockIfNeeded(Point windowPos)
        {
            var dockBorder = DockPanel.Parent as Border;
            if (dockBorder == null) return;

            if (IsPointerInDock(windowPos) && _dragSource == "grid" && _dockApps.Count < MaxDockApps)
            {
                dockBorder.BorderBrush = new SolidColorBrush(Color.FromArgb(150, 0, 122, 255));
                dockBorder.BorderThickness = new Thickness(2);
            }
            else
            {
                dockBorder.BorderBrush = null;
                dockBorder.BorderThickness = new Thickness(0);
            }
        }

        private void LongPressTimer_Tick(object sender, EventArgs e)
        {
            _longPressTimer.Stop();

            if (!_isEditMode)
            {
                EnterEditMode();

                // 如果有长按的目标，同时开始拖拽
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

            // 重新渲染带抖动动画的网格
            RenderAppGrid();
        }

        private void ExitEditMode()
        {
            _isEditMode = false;
            EditModeBanner.Visibility = Visibility.Collapsed;

            // 停止所有抖动动画
            foreach (var sb in _wiggleStoryboards)
            {
                sb.Stop();
            }
            _wiggleStoryboards.Clear();

            // 清除Dock高亮
            var dockBorder = DockPanel.Parent as Border;
            if (dockBorder != null)
            {
                dockBorder.BorderBrush = null;
                dockBorder.BorderThickness = new Thickness(0);
            }

            // 重新渲染（无动画）
            RenderAppGrid();
            UpdateDock();

            // 保存布局
            SaveCurrentLayout();
        }

        private void OnEditDoneClick(object sender, MouseButtonEventArgs e)
        {
            ExitEditMode();
        }

        /// <summary>
        /// 启动单个图标的抖动动画
        /// </summary>
        private void StartWiggleAnimation(StackPanel panel, int index)
        {
            var rotateTransform = new RotateTransform(0, CellWidth / 2, 10);
            panel.RenderTransform = rotateTransform;

            var animation = new DoubleAnimation
            {
                From = index % 2 == 0 ? -1.5 : 1.5,
                To = index % 2 == 0 ? 1.5 : -1.5,
                Duration = TimeSpan.FromMilliseconds(120 + (index % 3) * 30),
                AutoReverse = true,
                RepeatBehavior = RepeatBehavior.Forever,
                EasingFunction = new SineEase()
            };

            var storyboard = new Storyboard();
            Storyboard.SetTarget(animation, panel);
            Storyboard.SetTargetProperty(animation, new PropertyPath("(UIElement.RenderTransform).(RotateTransform.Angle)"));
            storyboard.Children.Add(animation);
            storyboard.Begin();

            _wiggleStoryboards.Add(storyboard);
        }

        /// <summary>
        /// 重新渲染除了指定索引之外的所有图标
        /// </summary>
        private void RenderAppGridExcept(int exceptIndex)
        {
            // 找到并保存被拖拽的元素
            StackPanel draggedPanel = null;
            foreach (UIElement child in AppGridCanvas.Children)
            {
                if (child is StackPanel sp && (int)sp.Tag == exceptIndex)
                {
                    // 这个可能不匹配，因为tag还没更新
                }
            }

            // 简单方式：更新除拖拽元素外所有元素的位置
            for (int i = 0; i < AppGridCanvas.Children.Count; i++)
            {
                if (AppGridCanvas.Children[i] is StackPanel sp)
                {
                    int oldIndex = (int)sp.Tag;
                    // 更新tag到新索引
                    int newIndex = _apps.FindIndex(a => a == _apps.FirstOrDefault(x => GetAppAtOldIndex(oldIndex, sp) != null));
                }
            }

            // 最简单的方式：全部重新渲染
            var tempDragged = _draggedElement;
            var tempDraggedIdx = _draggedIndex;
            var tempIsDragging = _isDragging;

            AppGridCanvas.Children.Clear();
            _wiggleStoryboards.Clear();

            for (int i = 0; i < _apps.Count; i++)
            {
                var app = _apps[i];
                var panel = CreateAppIconPanel(app, i);

                int row = i / Columns;
                int col = i % Columns;

                Canvas.SetLeft(panel, col * CellWidth);
                Canvas.SetTop(panel, row * CellHeight);

                if (i == tempDraggedIdx && tempIsDragging)
                {
                    panel.Opacity = 0.7;
                    Panel.SetZIndex(panel, 100);
                    panel.RenderTransform = new ScaleTransform(1.15, 1.15, CellWidth / 2, CellHeight / 2);
                    _draggedElement = panel;
                    // 保持在拖拽位置（由鼠标移动更新）
                }
                else if (_isEditMode)
                {
                    StartWiggleAnimation(panel, i);
                }

                AppGridCanvas.Children.Add(panel);
            }
        }

        private AppIconModel GetAppAtOldIndex(int index, StackPanel sp)
        {
            // Helper - not actually needed with full re-render approach
            return null;
        }

        #endregion

        #region 拖拽完成

        private void FinishDrag()
        {
            _isDragging = false;
            _draggedIndex = -1;

            if (_draggedElement != null)
            {
                _draggedElement.Opacity = 1;
                _draggedElement.RenderTransform = null;
                Panel.SetZIndex(_draggedElement, 0);
                _draggedElement = null;
            }

            // 重新渲染并保存
            RenderAppGrid();
            UpdateDock();
            SaveCurrentLayout();
        }

        private void AppGridCanvas_DragOver(object sender, DragEventArgs e)
        {
            e.Effects = DragDropEffects.Move;
            e.Handled = true;
        }

        private void AppGridCanvas_Drop(object sender, DragEventArgs e)
        {
            e.Handled = true;
        }

        #endregion

        #region 布局持久化

        private void SaveCurrentLayout()
        {
            var items = new List<Core.LayoutItem>();
            // 保存主屏幕网格
            for (int i = 0; i < _apps.Count; i++)
            {
                items.Add(new Core.LayoutItem
                {
                    AppId = _apps[i].Id,
                    Position = i,
                    Area = "grid"
                });
            }
            // 保存Dock栏
            for (int i = 0; i < _dockApps.Count; i++)
            {
                items.Add(new Core.LayoutItem
                {
                    AppId = _dockApps[i].Id,
                    Position = i,
                    Area = "dock"
                });
            }
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
            string temp = "25°";
            string desc = "多云";
            string icon = "⛅";
            string range = "H:29° L:21°";

            ClockCityText.Text = city;
            WeatherCityText.Text = city;
            WeatherTempText.Text = temp;
            WeatherIconText.Text = icon;
            WeatherDescText.Text = desc;
            WeatherRangeText.Text = range;
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

        private void OnSelectLockWallpaperClick(object sender, RoutedEventArgs e)
        {
            SelectWallpaper("lock");
        }

        private void OnSelectHomeWallpaperClick(object sender, RoutedEventArgs e)
        {
            SelectWallpaper("home");
        }

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
            {
                this.Background = new ImageBrush(new BitmapImage(new Uri(homePath))) { Stretch = Stretch.UniformToFill };
            }
            // 锁屏逻辑暂略，可在主界面状态切换时处理
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

        protected override void OnKeyDown(KeyEventArgs e)
        {
            if (e.Key == Key.Escape)
            {
                if (_isEditMode)
                {
                    ExitEditMode();
                    e.Handled = true;
                }
                else if (ActivationOverlay.Visibility == Visibility.Visible)
                {
                    ActivationOverlay.Visibility = Visibility.Collapsed;
                    e.Handled = true;
                }
                else if (ChatAppOverlay.Visibility == Visibility.Visible)
                {
                    ChatAppOverlay.Visibility = Visibility.Collapsed;
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