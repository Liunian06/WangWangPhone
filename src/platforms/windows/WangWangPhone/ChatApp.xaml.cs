
using System;
using System.Collections.Generic;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Shapes;

namespace WangWangPhone
{
    public class WXConversation
    {
        public string Id { get; set; }
        public string Name { get; set; }
        public string Avatar { get; set; }
        public string LastMsg { get; set; }
        public string Time { get; set; }
        public int Unread { get; set; }
        public bool Muted { get; set; }
        public Brush IconBg { get; set; }
    }

    public class WXContactGroup
    {
        public string Name { get; set; }
        public string Icon { get; set; }
        public Brush Color { get; set; }
    }

    public class WXContact
    {
        public string Id { get; set; }
        public string Name { get; set; }
        public string Avatar { get; set; }
        public string Letter { get; set; }
    }

    public class WXMoment
    {
        public string Name { get; set; }
        public string Avatar { get; set; }
        public string Content { get; set; }
        public string Time { get; set; }
        public List<string> Likes { get; set; }
        public List<(string, string)> Comments { get; set; }
    }

    public class WXChatMsg
    {
        public string Type { get; set; } // time, sent, received
        public string Name { get; set; }
        public string Avatar { get; set; }
        public string Text { get; set; }
    }

    public partial class ChatApp : UserControl
    {
        private string _currentTab = "messages";
        private string _currentChatId;
        private List<WXChatMsg> _currentMessages = new List<WXChatMsg>();

        public event Action OnCloseRequested;

        private static readonly List<WXConversation> Conversations = new List<WXConversation>
        {
            new WXConversation { Id="c1", Name="文件传输助手", Avatar="📁", LastMsg="你好，这是一条测试消息", Time="12:51", IconBg=new SolidColorBrush(Color.FromRgb(255,153,0)) },
            new WXConversation { Id="c2", Name="小明", Avatar="😊", LastMsg="[语音] 14\"", Time="11:38", Unread=2, IconBg=Brushes.Green },
            new WXConversation { Id="c3", Name="工作群", Avatar="👥", LastMsg="张三: 收到，谢谢大家的配合", Time="周四", Muted=true, IconBg=Brushes.Blue },
            new WXConversation { Id="c4", Name="家人群", Avatar="🏠", LastMsg="妈妈: [链接] 今日菜谱推荐...", Time="周三", IconBg=Brushes.HotPink },
            new WXConversation { Id="c5", Name="同学群", Avatar="🎓", LastMsg="李华: 科目一快考完了", Time="1月30日", IconBg=Brushes.Purple },
            new WXConversation { Id="c6", Name="技术交流群", Avatar="💻", LastMsg="王工: 新版本已部署上线", Time="12月2日", Muted=true, IconBg=Brushes.Gray },
            new WXConversation { Id="c7", Name="公众号", Avatar="📰", LastMsg="[3条] 今日科技资讯速览...", Time="16:37", Unread=3, IconBg=new SolidColorBrush(Color.FromRgb(25,117,210)) },
            new WXConversation { Id="c8", Name="服务号", Avatar="🔔", LastMsg="[5条通知] 您的快递已到达...", Time="16:30", Unread=5, IconBg=Brushes.Red },
            new WXConversation { Id="c9", Name="技术攻关群", Avatar="🔧", LastMsg="应该就好了", Time="16:11", IconBg=Brushes.Brown },
        };

        private static readonly List<WXContactGroup> ContactGroups = new List<WXContactGroup>
        {
            new WXContactGroup { Name="新的朋友", Icon="👤", Color=Brushes.Orange },
            new WXContactGroup { Name="仅聊天的朋友", Icon="👤", Color=Brushes.Orange },
            new WXContactGroup { Name="群聊", Icon="👥", Color=Brushes.Green },
            new WXContactGroup { Name="标签", Icon="🏷️", Color=Brushes.Blue },
            new WXContactGroup { Name="公众号", Icon="📰", Color=new SolidColorBrush(Color.FromRgb(25,117,210)) },
            new WXContactGroup { Name="服务号", Icon="🔔", Color=Brushes.Red },
        };

        private static readonly List<WXContact> StarredContacts = new List<WXContact>
        {
            new WXContact { Id="u1", Name="小明", Avatar="😊" },
            new WXContact { Id="u2", Name="小红", Avatar="🌸" },
        };

        private static readonly List<WXContact> ContactList = new List<WXContact>
        {
            new WXContact { Id="u3", Name="阿杰", Avatar="🧑", Letter="A" },
            new WXContact { Id="u4", Name="陈伟", Avatar="👨", Letter="C" },
            new WXContact { Id="u5", Name="大卫", Avatar="🧔", Letter="D" },
            new WXContact { Id="u6", Name="方琳", Avatar="👩", Letter="F" },
            new WXContact { Id="u7", Name="何志强", Avatar="👨‍💼", Letter="H" },
            new WXContact { Id="u8", Name="李华", Avatar="🧑‍🎓", Letter="L" },
            new WXContact { Id="u9", Name="马丽", Avatar="👩‍🦰", Letter="M" },
            new WXContact { Id="u1b", Name="小明", Avatar="😊", Letter="X" },
            new WXContact { Id="u2b", Name="小红", Avatar="🌸", Letter="X" },
            new WXContact { Id="u10", Name="张伟", Avatar="👨‍🔧", Letter="Z" },
            new WXContact { Id="u11", Name="赵敏", Avatar="👩‍🏫", Letter="Z" },
        };

        private static readonly List<WXMoment> Moments = new List<WXMoment>
        {
            new WXMoment { Name="小明", Avatar="😊", Content="今天天气真好，出去走走 🌞", Time="1分钟前", Likes=new List<string>{"小红","李华"}, Comments=new List<(string,string)>{("小红","确实不错！")} },
            new WXMoment { Name="李华", Avatar="🧑‍🎓", Content="终于把项目做完了，庆祝一下 🎉", Time="30分钟前", Likes=new List<string>{"小明","张伟","阿杰"}, Comments=new List<(string,string)>() },
            new WXMoment { Name="小红", Avatar="🌸", Content="分享一首好听的歌曲，推荐给大家～", Time="2小时前", Likes=new List<string>{"小明"}, Comments=new List<(string,string)>{("小明","什么歌？"),("小红","周杰伦的新专辑")} },
            new WXMoment { Name="张伟", Avatar="👨‍🔧", Content="周末去爬山，风景超美！大家有空可以一起来", Time="昨天", Likes=new List<string>{"小红","李华","马丽","陈伟"}, Comments=new List<(string,string)>{("马丽","哪个山？下次带我！")} },
        };

        private static readonly Dictionary<string, List<WXChatMsg>> ChatMessages = new Dictionary<string, List<WXChatMsg>>
        {
            ["c1"] = new List<WXChatMsg> {
                new WXChatMsg { Type="time", Text="昨天 16:04" },
                new WXChatMsg { Type="received", Name="文件传输助手", Avatar="📁", Text="你好，欢迎使用文件传输助手" },
                new WXChatMsg { Type="sent", Text="你好，这是一条测试消息" },
            },
            ["c2"] = new List<WXChatMsg> {
                new WXChatMsg { Type="time", Text="昨天 22:27" },
                new WXChatMsg { Type="received", Name="小明", Avatar="😊", Text="明天下午有空吗？一起去打球" },
                new WXChatMsg { Type="sent", Text="可以啊，几点？" },
                new WXChatMsg { Type="received", Name="小明", Avatar="😊", Text="下午三点吧" },
                new WXChatMsg { Type="sent", Text="好的，到时候见！" },
                new WXChatMsg { Type="time", Text="今天 11:30" },
                new WXChatMsg { Type="received", Name="小明", Avatar="😊", Text="[语音] 14\"" },
            },
            ["c3"] = new List<WXChatMsg> {
                new WXChatMsg { Type="time", Text="昨天 16:04" },
                new WXChatMsg { Type="received", Name="张三", Avatar="👨", Text="大家好，关于项目进度的问题" },
                new WXChatMsg { Type="received", Name="李四", Avatar="🧑", Text="我这边已经完成了80%" },
                new WXChatMsg { Type="sent", Text="收到" },
                new WXChatMsg { Type="time", Text="昨天 22:27" },
                new WXChatMsg { Type="received", Name="王五", Avatar="👨‍💼", Text="简单三步：先做设计、再写代码、最后测试" },
                new WXChatMsg { Type="received", Name="赵六", Avatar="🧔", Text="何意味" },
                new WXChatMsg { Type="received", Name="张三", Avatar="👨", Text="收到，谢谢大家的配合" },
            },
            ["c4"] = new List<WXChatMsg> {
                new WXChatMsg { Type="time", Text="周三 19:00" },
                new WXChatMsg { Type="received", Name="妈妈", Avatar="👩", Text="今天做了你最爱吃的红烧肉" },
                new WXChatMsg { Type="sent", Text="太棒了！我明天回去" },
                new WXChatMsg { Type="received", Name="爸爸", Avatar="👨", Text="路上注意安全" },
            },
        };

        public ChatApp()
        {
            InitializeComponent();
            RenderTabBar();
            RenderMessages();
        }

        private void OnCloseClick(object sender, MouseButtonEventArgs e)
        {
            OnCloseRequested?.Invoke();
        }

        #region Tab Navigation

        private void RenderTabBar()
        {
            TabBar.Children.Clear();
            int totalUnread = Conversations.Sum(c => c.Unread);
            var tabs = new[] {
                ("messages", "💬", "消息"),
                ("contacts", "👥", "通讯录"),
                ("moments", "📷", "朋友圈"),
                ("me", "👤", "我")
            };

            foreach (var (id, icon, label) in tabs)
            {
                var panel = new StackPanel { HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center, Cursor = Cursors.Hand };
                var iconGrid = new Grid();
                iconGrid.Children.Add(new TextBlock { Text = icon, FontSize = 22, HorizontalAlignment = HorizontalAlignment.Center });

                if (id == "messages" && totalUnread > 0)
                {
                    var badge = new Border
                    {
                        Background = Brushes.Red, CornerRadius = new CornerRadius(8), Padding = new Thickness(4, 1, 4, 1),
                        HorizontalAlignment = HorizontalAlignment.Right, VerticalAlignment = VerticalAlignment.Top,
                        Margin = new Thickness(0, -4, -8, 0)
                    };
                    badge.Child = new TextBlock { Text = totalUnread.ToString(), FontSize = 10, Foreground = Brushes.White };
                    iconGrid.Children.Add(badge);
                }

                panel.Children.Add(iconGrid);
                var isActive = _currentTab == id;
                panel.Children.Add(new TextBlock
                {
                    Text = label, FontSize = 10, HorizontalAlignment = HorizontalAlignment.Center,
                    Foreground = isActive ? new SolidColorBrush(Color.FromRgb(7, 193, 96)) : Brushes.Gray
                });

                string tabId = id;
                panel.MouseDown += (s, ev) => SwitchTab(tabId);
                TabBar.Children.Add(panel);
            }
        }

        private void SwitchTab(string tab)
        {
            _currentTab = tab;
            var titles = new Dictionary<string, string> { ["messages"] = "消息", ["contacts"] = "通讯录", ["moments"] = "朋友圈", ["me"] = "我" };
            HeaderTitle.Text = titles.GetValueOrDefault(tab, "");
            RenderTabBar();
            ContentPanel.Children.Clear();

            switch (tab)
            {
                case "messages": RenderMessages(); break;
                case "contacts": RenderContacts(); break;
                case "moments": RenderMoments(); break;
                case "me": RenderMe(); break;
            }
        }

        #endregion

        #region Messages Tab

        private void RenderMessages()
        {
            ContentPanel.Children.Clear();
            foreach (var conv in Conversations)
            {
                var row = new Grid { Background = Brushes.White, Cursor = Cursors.Hand, Margin = new Thickness(0, 0, 0, 0) };
                row.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(60) });
                row.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

                // Avatar
                var avatarGrid = new Grid { Margin = new Thickness(12) };
                var avatarBg = new Border { Width = 48, Height = 48, CornerRadius = new CornerRadius(6), Background = conv.IconBg };
                avatarBg.Child = new TextBlock { Text = conv.Avatar, FontSize = 24, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center };
                avatarGrid.Children.Add(avatarBg);

                if (conv.Unread > 0)
                {
                    var badge = new Border { Background = Brushes.Red, CornerRadius = new CornerRadius(8), Padding = new Thickness(4, 1, 4, 1), HorizontalAlignment = HorizontalAlignment.Right, VerticalAlignment = VerticalAlignment.Top, Margin = new Thickness(0, -2, -2, 0) };
                    badge.Child = new TextBlock { Text = conv.Unread.ToString(), FontSize = 10, Foreground = Brushes.White };
                    avatarGrid.Children.Add(badge);
                }
                Grid.SetColumn(avatarGrid, 0);
                row.Children.Add(avatarGrid);

                // Content
                var content = new StackPanel { VerticalAlignment = VerticalAlignment.Center, Margin = new Thickness(8, 12, 12, 12) };
                var topRow = new Grid();
                topRow.Children.Add(new TextBlock { Text = conv.Name, FontSize = 16, HorizontalAlignment = HorizontalAlignment.Left });
                topRow.Children.Add(new TextBlock { Text = conv.Time, FontSize = 12, Foreground = new SolidColorBrush(Color.FromRgb(180, 180, 180)), HorizontalAlignment = HorizontalAlignment.Right });
                content.Children.Add(topRow);

                var bottomRow = new Grid { Margin = new Thickness(0, 4, 0, 0) };
                bottomRow.Children.Add(new TextBlock { Text = conv.LastMsg, FontSize = 14, Foreground = new SolidColorBrush(Color.FromRgb(180, 180, 180)), TextTrimming = TextTrimming.CharacterEllipsis, HorizontalAlignment = HorizontalAlignment.Left, MaxWidth = 220 });
                if (conv.Muted) bottomRow.Children.Add(new TextBlock { Text = "🔇", FontSize = 14, HorizontalAlignment = HorizontalAlignment.Right });
                content.Children.Add(bottomRow);

                Grid.SetColumn(content, 1);
                row.Children.Add(content);

                string chatId = conv.Id;
                row.MouseDown += (s, ev) => OpenChatDetail(chatId);
                ContentPanel.Children.Add(row);

                // Divider
                ContentPanel.Children.Add(new Border { Height = 0.5, Background = new SolidColorBrush(Color.FromRgb(230, 230, 230)), Margin = new Thickness(72, 0, 0, 0) });
            }
        }

        #endregion

        #region Contacts Tab

        private void RenderContacts()
        {
            ContentPanel.Children.Clear();

            foreach (var g in ContactGroups)
            {
                var row = new StackPanel { Orientation = Orientation.Horizontal, Background = Brushes.White, Margin = new Thickness(0) };
                var iconBg = new Border { Width = 40, Height = 40, CornerRadius = new CornerRadius(6), Background = g.Color, Margin = new Thickness(12) };
                iconBg.Child = new TextBlock { Text = g.Icon, FontSize = 20, Foreground = Brushes.White, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center };
                row.Children.Add(iconBg);
                row.Children.Add(new TextBlock { Text = g.Name, FontSize = 16, VerticalAlignment = VerticalAlignment.Center });
                ContentPanel.Children.Add(row);
                ContentPanel.Children.Add(new Border { Height = 0.5, Background = new SolidColorBrush(Color.FromRgb(230, 230, 230)) });
            }

            // Starred
            AddSectionHeader("星标朋友");
            foreach (var c in StarredContacts) AddContactRow(c);

            // Contact List
            string lastLetter = "";
            foreach (var c in ContactList)
            {
                if (!string.IsNullOrEmpty(c.Letter) && c.Letter != lastLetter)
                {
                    lastLetter = c.Letter;
                    AddSectionHeader(c.Letter);
                }
                AddContactRow(c);
            }
        }

        private void AddSectionHeader(string text)
        {
            ContentPanel.Children.Add(new TextBlock
            {
                Text = text, FontSize = 13, Foreground = Brushes.Gray,
                Margin = new Thickness(16, 6, 16, 6),
                Background = new SolidColorBrush(Color.FromRgb(237, 237, 237))
            });
        }

        private void AddContactRow(WXContact contact)
        {
            var row = new StackPanel { Orientation = Orientation.Horizontal, Background = Brushes.White, Cursor = Cursors.Hand };
            var avatar = new Border { Width = 40, Height = 40, CornerRadius = new CornerRadius(6), Background = new SolidColorBrush(Color.FromRgb(245, 245, 245)), Margin = new Thickness(16, 10, 12, 10) };
            avatar.Child = new TextBlock { Text = contact.Avatar, FontSize = 22, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center };
            row.Children.Add(avatar);
            row.Children.Add(new TextBlock { Text = contact.Name, FontSize = 16, VerticalAlignment = VerticalAlignment.Center });

            row.MouseDown += (s, ev) =>
            {
                var conv = Conversations.FirstOrDefault(x => x.Name == contact.Name);
                OpenChatDetail(conv?.Id ?? $"u_{contact.Id}");
            };
            ContentPanel.Children.Add(row);
            ContentPanel.Children.Add(new Border { Height = 0.5, Background = new SolidColorBrush(Color.FromRgb(230, 230, 230)) });
        }

        #endregion

        #region Moments Tab

        private void RenderMoments()
        {
            ContentPanel.Children.Clear();

            // Cover
            var cover = new Grid { Height = 250 };
            cover.Background = new LinearGradientBrush(Color.FromRgb(102, 125, 235), Color.FromRgb(117, 74, 163), 45);
            var coverContent = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Right, VerticalAlignment = VerticalAlignment.Bottom, Margin = new Thickness(16) };
            coverContent.Children.Add(new TextBlock { Text = "我的昵称", Foreground = Brushes.White, FontSize = 18, FontWeight = FontWeights.SemiBold, VerticalAlignment = VerticalAlignment.Center, Margin = new Thickness(0, 0, 12, 0) });
            var profilePic = new Border { Width = 64, Height = 64, CornerRadius = new CornerRadius(10), Background = new SolidColorBrush(Color.FromRgb(245, 245, 220)), BorderBrush = Brushes.White, BorderThickness = new Thickness(2) };
            profilePic.Child = new TextBlock { Text = "🐱", FontSize = 32, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center };
            coverContent.Children.Add(profilePic);
            cover.Children.Add(coverContent);
            ContentPanel.Children.Add(cover);

            // Moments list
            foreach (var m in Moments)
            {
                var momentPanel = new Grid { Background = Brushes.White, Margin = new Thickness(0, 0, 0, 0) };
                momentPanel.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(52) });
                momentPanel.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

                var mAvatar = new Border { Width = 40, Height = 40, CornerRadius = new CornerRadius(6), Background = new SolidColorBrush(Color.FromRgb(245, 245, 245)), Margin = new Thickness(12, 12, 0, 12), VerticalAlignment = VerticalAlignment.Top };
                mAvatar.Child = new TextBlock { Text = m.Avatar, FontSize = 22, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center };
                Grid.SetColumn(mAvatar, 0);
                momentPanel.Children.Add(mAvatar);

                var mContent = new StackPanel { Margin = new Thickness(10, 12, 12, 12) };
                mContent.Children.Add(new TextBlock { Text = m.Name, FontSize = 15, FontWeight = FontWeights.SemiBold, Foreground = new SolidColorBrush(Color.FromRgb(87, 107, 149)) });
                mContent.Children.Add(new TextBlock { Text = m.Content, FontSize = 15, TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 4, 0, 4) });

                var timeRow = new Grid { Margin = new Thickness(0, 2, 0, 4) };
                timeRow.Children.Add(new TextBlock { Text = m.Time, FontSize = 12, Foreground = new SolidColorBrush(Color.FromRgb(180, 180, 180)) });
                timeRow.Children.Add(new TextBlock { Text = "··", FontSize = 14, HorizontalAlignment = HorizontalAlignment.Right, Foreground = new SolidColorBrush(Color.FromRgb(87, 107, 149)) });
                mContent.Children.Add(timeRow);

                if (m.Likes.Count > 0 || m.Comments.Count > 0)
                {
                    var interactionBg = new Border { Background = new SolidColorBrush(Color.FromRgb(247, 247, 247)), CornerRadius = new CornerRadius(4), Padding = new Thickness(6), Margin = new Thickness(0, 4, 0, 0) };
                    var interactionPanel = new StackPanel();
                    if (m.Likes.Count > 0)
                        interactionPanel.Children.Add(new TextBlock { Text = "❤️ " + string.Join("，", m.Likes), FontSize = 13, Foreground = new SolidColorBrush(Color.FromRgb(87, 107, 149)), TextWrapping = TextWrapping.Wrap });
                    if (m.Likes.Count > 0 && m.Comments.Count > 0)
                        interactionPanel.Children.Add(new Border { Height = 0.5, Background = new SolidColorBrush(Color.FromRgb(220, 220, 220)), Margin = new Thickness(0, 4, 0, 4) });
                    foreach (var (author, text) in m.Comments)
                    {
                        var commentRow = new TextBlock { FontSize = 13, TextWrapping = TextWrapping.Wrap };
                        commentRow.Inlines.Add(new System.Windows.Documents.Run(author) { FontWeight = FontWeights.Medium, Foreground = new SolidColorBrush(Color.FromRgb(87, 107, 149)) });
                        commentRow.Inlines.Add(new System.Windows.Documents.Run($"：{text}"));
                        interactionPanel.Children.Add(commentRow);
                    }
                    interactionBg.Child = interactionPanel;
                    mContent.Children.Add(interactionBg);
                }

                Grid.SetColumn(mContent, 1);
                momentPanel.Children.Add(mContent);
                ContentPanel.Children.Add(momentPanel);
                ContentPanel.Children.Add(new Border { Height = 0.5, Background = new SolidColorBrush(Color.FromRgb(230, 230, 230)) });
            }
        }

        #endregion

        #region Me Tab

        private void RenderMe()
        {
            ContentPanel.Children.Clear();

            // Profile card
            var profileCard = new StackPanel { Background = Brushes.White };
            var profileRow = new StackPanel { Orientation = Orientation.Horizontal, Margin = new Thickness(16, 24, 16, 8) };
            var profileAvatar = new Border { Width = 64, Height = 64, CornerRadius = new CornerRadius(10), Background = new SolidColorBrush(Color.FromRgb(245, 245, 220)) };
            profileAvatar.Child = new TextBlock { Text = "🐱", FontSize = 32, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center };
            profileRow.Children.Add(profileAvatar);
            var profileInfo = new StackPanel { Margin = new Thickness(14, 0, 0, 0), VerticalAlignment = VerticalAlignment.Center };
            profileInfo.Children.Add(new TextBlock { Text = "我的昵称", FontSize = 18, FontWeight = FontWeights.SemiBold });
            profileInfo.Children.Add(new TextBlock { Text = "账号：WangWang_User", FontSize = 14, Foreground = Brushes.Gray, Margin = new Thickness(0, 4, 0, 0) });
            profileRow.Children.Add(profileInfo);
            profileCard.Children.Add(profileRow);

            var statusRow = new StackPanel { Orientation = Orientation.Horizontal, Margin = new Thickness(16, 0, 16, 16) };
            statusRow.Children.Add(CreatePill("+ 状态"));
            statusRow.Children.Add(CreatePill("👤👤👤 等40个朋友 ●"));
            profileCard.Children.Add(statusRow);
            ContentPanel.Children.Add(profileCard);

            // Menu groups
            var menuGroups = new List<List<(string icon, string label, Action action)>>
            {
                new List<(string, string, Action)> { ("✅", "服务", () => ShowService()) },
                new List<(string, string, Action)> { ("⭐", "收藏", null), ("🖼️", "朋友圈", () => SwitchTab("moments")), ("📺", "视频号和公众号", null), ("🛒", "订单与卡包", null), ("😊", "表情", null) },
                new List<(string, string, Action)> { ("⚙️", "设置", null) },
            };

            foreach (var group in menuGroups)
            {
                ContentPanel.Children.Add(new Border { Height = 8, Background = new SolidColorBrush(Color.FromRgb(237, 237, 237)) });
                for (int i = 0; i < group.Count; i++)
                {
                    var item = group[i];
                    var menuRow = new Grid { Background = Brushes.White, Cursor = Cursors.Hand, Height = 50 };
                    var menuContent = new StackPanel { Orientation = Orientation.Horizontal, VerticalAlignment = VerticalAlignment.Center, Margin = new Thickness(14, 0, 0, 0) };
                    menuContent.Children.Add(new TextBlock { Text = item.icon, FontSize = 20, Width = 30 });
                    menuContent.Children.Add(new TextBlock { Text = item.label, FontSize = 16 });
                    menuRow.Children.Add(menuContent);
                    menuRow.Children.Add(new TextBlock { Text = "›", Foreground = new SolidColorBrush(Color.FromRgb(190, 190, 190)), FontSize = 14, HorizontalAlignment = HorizontalAlignment.Right, VerticalAlignment = VerticalAlignment.Center, Margin = new Thickness(0, 0, 14, 0) });

                    if (item.action != null)
                    {
                        var act = item.action;
                        menuRow.MouseDown += (s, ev) => act();
                    }
                    ContentPanel.Children.Add(menuRow);
                    if (i < group.Count - 1)
                        ContentPanel.Children.Add(new Border { Height = 0.5, Background = new SolidColorBrush(Color.FromRgb(230, 230, 230)) });
                }
            }
        }

        private Border CreatePill(string text)
        {
            var pill = new Border
            {
                BorderBrush = new SolidColorBrush(Color.FromRgb(225, 225, 225)),
                BorderThickness = new Thickness(0.5),
                CornerRadius = new CornerRadius(16),
                Padding = new Thickness(12, 4, 12, 4),
                Margin = new Thickness(0, 0, 10, 0)
            };
            pill.Child = new TextBlock { Text = text, FontSize = 13, Foreground = Brushes.Gray };
            return pill;
        }

        #endregion

        #region Chat Detail

        private void OpenChatDetail(string chatId)
        {
            _currentChatId = chatId;
            var conv = Conversations.FirstOrDefault(c => c.Id == chatId);
            string chatName = conv?.Name ?? "聊天";

            // Load messages
            if (ChatMessages.ContainsKey(chatId))
                _currentMessages = new List<WXChatMsg>(ChatMessages[chatId]);
            else
                _currentMessages = new List<WXChatMsg>
                {
                    new WXChatMsg { Type = "time", Text = "今天 12:00" },
                    new WXChatMsg { Type = "received", Name = conv?.Name ?? "对方", Avatar = conv?.Avatar ?? "👤", Text = "你好！" }
                };

            // Determine if group
            bool isGroup = chatName.Contains("群") ||
                _currentMessages.Where(m => m.Type == "received").Select(m => m.Name).Distinct().Count() > 1;

            string title = isGroup ? $"{chatName}({new Random().Next(10, 50)})" : chatName;
            ChatDetailTitle.Text = title;

            MainView.Visibility = Visibility.Collapsed;
            ChatDetailView.Visibility = Visibility.Visible;

            RenderChatMessages(isGroup);
        }

        private void RenderChatMessages(bool isGroup)
        {
            ChatMessagesPanel.Children.Clear();

            foreach (var msg in _currentMessages)
            {
                switch (msg.Type)
                {
                    case "time":
                        ChatMessagesPanel.Children.Add(new TextBlock
                        {
                            Text = msg.Text, FontSize = 12,
                            Foreground = new SolidColorBrush(Color.FromRgb(180, 180, 180)),
                            HorizontalAlignment = HorizontalAlignment.Center,
                            Margin = new Thickness(0, 6, 0, 6)
                        });
                        break;

                    case "sent":
                        var sentRow = new Grid { Margin = new Thickness(0, 4, 0, 4) };
                        var sentBubble = new Border
                        {
                            Background = new SolidColorBrush(Color.FromRgb(149, 236, 105)),
                            CornerRadius = new CornerRadius(6),
                            Padding = new Thickness(10),
                            HorizontalAlignment = HorizontalAlignment.Right,
                            MaxWidth = 240,
                            Margin = new Thickness(0, 0, 44, 0)
                        };
                        sentBubble.Child = new TextBlock { Text = msg.Text, FontSize = 15, TextWrapping = TextWrapping.Wrap };
                        sentRow.Children.Add(sentBubble);

                        var sentAvatar = new Border
                        {
                            Width = 36, Height = 36,
                            CornerRadius = new CornerRadius(6),
                            Background = new SolidColorBrush(Color.FromRgb(245, 245, 245)),
                            HorizontalAlignment = HorizontalAlignment.Right,
                            VerticalAlignment = VerticalAlignment.Top
                        };
                        sentAvatar.Child = new TextBlock { Text = "🐱", FontSize = 18, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center };
                        sentRow.Children.Add(sentAvatar);

                        ChatMessagesPanel.Children.Add(sentRow);
                        break;

                    case "received":
                        var recvRow = new Grid { Margin = new Thickness(0, 4, 0, 4) };

                        var recvAvatar = new Border
                        {
                            Width = 36, Height = 36,
                            CornerRadius = new CornerRadius(6),
                            Background = new SolidColorBrush(Color.FromRgb(245, 245, 245)),
                            HorizontalAlignment = HorizontalAlignment.Left,
                            VerticalAlignment = VerticalAlignment.Top
                        };
                        recvAvatar.Child = new TextBlock { Text = string.IsNullOrEmpty(msg.Avatar) ? "👤" : msg.Avatar, FontSize = 18, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center };
                        recvRow.Children.Add(recvAvatar);

                        var recvContent = new StackPanel { HorizontalAlignment = HorizontalAlignment.Left, Margin = new Thickness(44, 0, 0, 0) };
                        if (isGroup)
                        {
                            recvContent.Children.Add(new TextBlock { Text = msg.Name ?? "", FontSize = 12, Foreground = Brushes.Gray, Margin = new Thickness(0, 0, 0, 2) });
                        }
                        var recvBubble = new Border
                        {
                            Background = Brushes.White,
                            CornerRadius = new CornerRadius(6),
                            Padding = new Thickness(10),
                            MaxWidth = 240
                        };
                        recvBubble.Child = new TextBlock { Text = msg.Text, FontSize = 15, TextWrapping = TextWrapping.Wrap };
                        recvContent.Children.Add(recvBubble);
                        recvRow.Children.Add(recvContent);

                        ChatMessagesPanel.Children.Add(recvRow);
                        break;
                }
            }

            // Scroll to bottom
            ChatMessagesScroll.ScrollToEnd();
        }

        private void OnChatDetailBack(object sender, MouseButtonEventArgs e)
        {
            ChatDetailView.Visibility = Visibility.Collapsed;
            MainView.Visibility = Visibility.Visible;
            _currentChatId = null;
        }

        private void OnSendMessage(object sender, MouseButtonEventArgs e)
        {
            string text = ChatInputBox.Text?.Trim();
            if (string.IsNullOrEmpty(text)) return;

            _currentMessages.Add(new WXChatMsg { Type = "sent", Text = text });
            ChatInputBox.Text = "";

            var conv = Conversations.FirstOrDefault(c => c.Id == _currentChatId);
            bool isGroup = (conv?.Name?.Contains("群") ?? false) ||
                _currentMessages.Where(m => m.Type == "received").Select(m => m.Name).Distinct().Count() > 1;

            RenderChatMessages(isGroup);
        }

        #endregion

        #region Service Page

        private void ShowService()
        {
            MainView.Visibility = Visibility.Collapsed;
            ServiceView.Visibility = Visibility.Visible;
        }

        private void OnServiceBack(object sender, MouseButtonEventArgs e)
        {
            ServiceView.Visibility = Visibility.Collapsed;
            MainView.Visibility = Visibility.Visible;
        }

        #endregion
    }
}