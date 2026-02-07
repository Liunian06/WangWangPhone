
/**
 * 聊天App模块 - 仿微信界面
 * 包含：消息列表、通讯录、朋友圈、我的页面、聊天详情、服务页
 */

// ============================================
// 模拟数据
// ============================================
const MOCK_CONVERSATIONS = [
    { id: 'c1', name: '文件传输助手', avatar: '📁', lastMsg: '你好，这是一条测试消息', time: '12:51', unread: 0, icon_bg: '#FF9800' },
    { id: 'c2', name: '小明', avatar: '😊', lastMsg: '[语音] 14"', time: '11:38', unread: 2, icon_bg: '#4CAF50' },
    { id: 'c3', name: '工作群', avatar: '👥', lastMsg: '张三: 收到，谢谢大家的配合', time: '周四', unread: 0, muted: true, icon_bg: '#2196F3' },
    { id: 'c4', name: '家人群', avatar: '🏠', lastMsg: '妈妈: [链接] 今日菜谱推荐...', time: '周三', unread: 0, icon_bg: '#E91E63' },
    { id: 'c5', name: '同学群', avatar: '🎓', lastMsg: '李华: 科目一快考完了', time: '1月30日', unread: 0, icon_bg: '#9C27B0' },
    { id: 'c6', name: '技术交流群', avatar: '💻', lastMsg: '王工: 新版本已经部署上线', time: '12月2日', unread: 0, muted: true, icon_bg: '#607D8B' },
    { id: 'c7', name: '公众号', avatar: '📰', lastMsg: '[3条] 今日科技资讯速览...', time: '16:37', unread: 3, icon_bg: '#1976D2' },
    { id: 'c8', name: '服务号', avatar: '🔔', lastMsg: '[5条通知] 您的快递已到达...', time: '16:30', unread: 5, icon_bg: '#D32F2F' },
    { id: 'c9', name: '技术攻关群', avatar: '🔧', lastMsg: '应该就好了', time: '16:11', unread: 0, icon_bg: '#795548' },
];

const MOCK_CONTACTS_GROUPS = [
    { id: 'g1', name: '新的朋友', icon: '👤', color: '#FF9800' },
    { id: 'g2', name: '仅聊天的朋友', icon: '👤', color: '#FF9800' },
    { id: 'g3', name: '群聊', icon: '👥', color: '#4CAF50' },
    { id: 'g4', name: '标签', icon: '🏷️', color: '#2196F3' },
    { id: 'g5', name: '公众号', icon: '📰', color: '#1976D2' },
    { id: 'g6', name: '服务号', icon: '🔔', color: '#D32F2F' },
];

const MOCK_STARRED = [
    { id: 'u1', name: '小明', avatar: '😊' },
    { id: 'u2', name: '小红', avatar: '🌸' },
];

const MOCK_CONTACT_LIST = [
    { id: 'u3', name: '阿杰', avatar: '🧑', letter: 'A' },
    { id: 'u4', name: '陈伟', avatar: '👨', letter: 'C' },
    { id: 'u5', name: '大卫', avatar: '🧔', letter: 'D' },
    { id: 'u6', name: '方琳', avatar: '👩', letter: 'F' },
    { id: 'u7', name: '何志强', avatar: '👨‍💼', letter: 'H' },
    { id: 'u8', name: '李华', avatar: '🧑‍🎓', letter: 'L' },
    { id: 'u9', name: '马丽', avatar: '👩‍🦰', letter: 'M' },
    { id: 'u1b', name: '小明', avatar: '😊', letter: 'X' },
    { id: 'u2b', name: '小红', avatar: '🌸', letter: 'X' },
    { id: 'u10', name: '张伟', avatar: '👨‍🔧', letter: 'Z' },
    { id: 'u11', name: '赵敏', avatar: '👩‍🏫', letter: 'Z' },
];

const MOCK_MOMENTS = [
    { id: 'm1', name: '小明', avatar: '😊', content: '今天天气真好，出去走走 🌞', time: '1分钟前', likes: ['小红', '李华'], comments: [{ name: '小红', text: '确实不错！' }] },
    { id: 'm2', name: '李华', avatar: '🧑‍🎓', content: '终于把项目做完了，庆祝一下 🎉', time: '30分钟前', likes: ['小明', '张伟', '阿杰'], comments: [] },
    { id: 'm3', name: '小红', avatar: '🌸', content: '分享一首好听的歌曲，推荐给大家～', time: '2小时前', likes: ['小明'], comments: [{ name: '小明', text: '什么歌？' }, { name: '小红', text: '周杰伦的新专辑' }] },
    { id: 'm4', name: '张伟', avatar: '👨‍🔧', content: '周末去爬山，风景超美！大家有空可以一起来', time: '昨天', likes: ['小红', '李华', '马丽', '陈伟'], comments: [{ name: '马丽', text: '哪个山？下次带我！' }] },
];

const MOCK_CHAT_MESSAGES = {
    'c1': [
        { type: 'time', text: '昨天 16:04' },
        { type: 'received', name: '文件传输助手', avatar: '📁', text: '你好，欢迎使用文件传输助手' },
        { type: 'sent', text: '你好，这是一条测试消息' },
    ],
    'c2': [
        { type: 'time', text: '昨天 22:27' },
        { type: 'received', name: '小明', avatar: '😊', text: '明天下午有空吗？一起去打球' },
        { type: 'sent', text: '可以啊，几点？' },
        { type: 'received', name: '小明', avatar: '😊', text: '下午三点吧' },
        { type: 'sent', text: '好的，到时候见！' },
        { type: 'time', text: '今天 11:30' },
        { type: 'received', name: '小明', avatar: '😊', text: '[语音] 14"' },
    ],
    'c3': [
        { type: 'time', text: '昨天 16:04' },
        { type: 'received', name: '张三', avatar: '👨', text: '大家好，关于项目进度的问题' },
        { type: 'received', name: '李四', avatar: '🧑', text: '我这边已经完成了80%' },
        { type: 'sent', text: '收到' },
        { type: 'time', text: '昨天 22:27' },
        { type: 'received', name: '王五', avatar: '👨‍💼', text: '简单三步：先做设计、再写代码、最后测试' },
        { type: 'received', name: '赵六', avatar: '🧔', text: '何意味' },
        { type: 'received', name: '张三', avatar: '👨', text: '收到，谢谢大家的配合' },
    ],
    'c4': [
        { type: 'time', text: '周三 19:00' },
        { type: 'received', name: '妈妈', avatar: '👩', text: '今天做了你最爱吃的红烧肉' },
        { type: 'sent', text: '太棒了！我明天回去' },
        { type: 'received', name: '爸爸', avatar: '👨', text: '路上注意安全' },
    ],
};

// ============================================
// 聊天App主类
// ============================================
export class ChatApp {
    constructor(container, onClose) {
        this.container = container;
        this.onClose = onClose;
        this.currentTab = 'messages';
        this.currentView = 'main';
        this.currentChatId = null;
        this.render();
    }

    destroy() {
        this.container.innerHTML = '';
    }

    render() {
        this.container.innerHTML = '';
        switch (this.currentView) {
            case 'chat-detail':
                this.renderChatDetail();
                break;
            case 'service':
                this.renderServicePage();
                break;
            default:
                this.renderMainView();
                break;
        }
    }

    // ============================================
    // 主视图（含底部Tab）
    // ============================================
    renderMainView() {
        const app = this.el('div', 'chat-app');

        const titles = { messages: '消息', contacts: '通讯录', moments: '朋友圈', me: '我' };

        // Header
        const header = this.el('div', 'chat-header');
        const backBtn = this.el('div', 'chat-header-left');
        backBtn.innerHTML = '‹';
        backBtn.onclick = () => { this.destroy(); if (this.onClose) this.onClose(); };

        const title = this.el('div', 'chat-header-title');
        title.textContent = titles[this.currentTab];

        const rightBtns = this.el('div', 'chat-header-right');
        rightBtns.innerHTML = '<span class="chat-header-icon">🔍</span><span class="chat-header-icon">⊕</span>';

        header.append(backBtn, title, rightBtns);
        app.appendChild(header);

        // Content
        const content = this.el('div', 'chat-content');
        app.appendChild(content);

        // Tab bar
        const tabBar = this.createTabBar();
        app.appendChild(tabBar);

        this.container.appendChild(app);
        this.renderTabContent(content);
    }

    createTabBar() {
        const bar = this.el('div', 'chat-tab-bar');
        const totalUnread = MOCK_CONVERSATIONS.reduce((s, c) => s + (c.unread || 0), 0);

        const tabs = [
            { id: 'messages', icon: '💬', label: '消息', badge: totalUnread },
            { id: 'contacts', icon: '👥', label: '通讯录', badge: 0 },
            { id: 'moments', icon: '📷', label: '朋友圈', badge: 0 },
            { id: 'me', icon: '👤', label: '我', badge: 0 },
        ];

        tabs.forEach(t => {
            const item = this.el('div', 'chat-tab-item' + (this.currentTab === t.id ? ' active' : ''));
            let html = '<span class="chat-tab-icon">' + t.icon + '</span>';
            html += '<span class="chat-tab-label">' + t.label + '</span>';
            if (t.badge > 0) html += '<span class="chat-tab-badge">' + t.badge + '</span>';
            item.innerHTML = html;
            item.onclick = () => { this.currentTab = t.id; this.render(); };
            bar.appendChild(item);
        });

        return bar;
    }

    renderTabContent(content) {
        switch (this.currentTab) {
            case 'messages': this.renderMessages(content); break;
            case 'contacts': this.renderContacts(content); break;
            case 'moments': this.renderMoments(content); break;
            case 'me': this.renderMe(content); break;
        }
    }

    // ============================================
    // Tab1: 消息列表
    // ============================================
    renderMessages(content) {
        MOCK_CONVERSATIONS.forEach(conv => {
            const item = this.el('div', 'msg-list-item');

            const avatarDiv = this.el('div', 'msg-avatar');
            avatarDiv.style.background = conv.icon_bg;
            avatarDiv.textContent = conv.avatar;

            if (conv.unread > 0) {
                const badge = this.el('span', 'msg-avatar-badge');
                badge.textContent = conv.unread;
                avatarDiv.appendChild(badge);
            }

            const info = this.el('div', 'msg-info');
            const topRow = this.el('div', 'msg-top-row');
            const name = this.el('span', 'msg-name');
            name.textContent = conv.name;
            const time = this.el('span', 'msg-time');
            time.textContent = conv.time;
            topRow.append(name, time);

            const preview = this.el('div', 'msg-preview');
            const msgText = this.el('span');
            msgText.textContent = conv.lastMsg;
            preview.appendChild(msgText);
            if (conv.muted) {
                const muteIcon = this.el('span', 'msg-muted-icon');
                muteIcon.textContent = '🔇';
                preview.appendChild(muteIcon);
            }

            info.append(topRow, preview);
            item.append(avatarDiv, info);

            item.onclick = () => {
                this.currentChatId = conv.id;
                this.currentView = 'chat-detail';
                this.render();
            };

            content.appendChild(item);
        });
    }

    // ============================================
    // Tab2: 通讯录
    // ============================================
    renderContacts(content) {
        const wrapper = this.el('div');
        wrapper.style.position = 'relative';

        // 功能入口
        MOCK_CONTACTS_GROUPS.forEach(g => {
            const item = this.el('div', 'contact-func-item');
            const icon = this.el('div', 'contact-func-icon');
            icon.style.background = g.color;
            icon.textContent = g.icon;
            const name = this.el('div', 'contact-func-name');
            name.textContent = g.name;
            item.append(icon, name);
            wrapper.appendChild(item);
        });

        // 星标朋友
        if (MOCK_STARRED.length > 0) {
            const h = this.el('div', 'contacts-section-header');
            h.textContent = '星标朋友';
            wrapper.appendChild(h);
            MOCK_STARRED.forEach(c => wrapper.appendChild(this.createContactItem(c)));
        }

        // 按字母分组
        let curLetter = '';
        MOCK_CONTACT_LIST.forEach(c => {
            if (c.letter && c.letter !== curLetter) {
                curLetter = c.letter;
                const h = this.el('div', 'contacts-section-header');
                h.textContent = curLetter;
                h.id = 'letter-' + curLetter;
                wrapper.appendChild(h);
            }
            wrapper.appendChild(this.createContactItem(c));
        });

        content.appendChild(wrapper);

        // 右侧字母索引
        const letters = ['↑', '☆'];
        const uniqueLetters = [...new Set(MOCK_CONTACT_LIST.map(c => c.letter).filter(Boolean))];
        letters.push(...uniqueLetters, '#');

        const indexBar = this.el('div', 'letter-index');
        letters.forEach(l => {
            const li = this.el('div', 'letter-index-item');
            li.textContent = l;
            li.onclick = () => {
                const el = document.getElementById('letter-' + l);
                if (el) el.scrollIntoView({ behavior: 'smooth' });
            };
            indexBar.appendChild(li);
        });
        content.style.position = 'relative';
        content.appendChild(indexBar);
    }

    createContactItem(contact) {
        const item = this.el('div', 'contact-item');
        const avatar = this.el('div', 'contact-avatar');
        avatar.textContent = contact.avatar;
        const name = this.el('div', 'contact-name');
        name.textContent = contact.name;
        item.append(avatar, name);

        item.onclick = () => {
            const conv = MOCK_CONVERSATIONS.find(c => c.name === contact.name);
            this.currentChatId = conv ? conv.id : 'u_' + contact.id;
            this.currentView = 'chat-detail';
            this.render();
        };
        return item;
    }

    // ============================================
    // Tab3: 朋友圈
    // ============================================
    renderMoments(content) {
        // 封面区
        const cover = this.el('div', 'moments-cover');
        const userArea = this.el('div', 'moments-cover-user');
        const coverName = this.el('span', 'moments-cover-name');
        coverName.textContent = '我的昵称';
        const coverAvatar = this.el('div', 'moments-cover-avatar');
        coverAvatar.textContent = '🐱';
        userArea.append(coverName, coverAvatar);
        cover.appendChild(userArea);
        content.appendChild(cover);

        // 动态列表
        MOCK_MOMENTS.forEach(m => {
            const item = this.el('div', 'moment-item');
            const header = this.el('div', 'moment-header');

            const avatar = this.el('div', 'moment-avatar');
            avatar.textContent = m.avatar;

            const body = this.el('div', 'moment-body');

            const nameEl = this.el('div', 'moment-name');
            nameEl.textContent = m.name;

            const textEl = this.el('div', 'moment-text');
            textEl.textContent = m.content;

            const footer = this.el('div', 'moment-footer');
            const timeEl = this.el('span', 'moment-time');
            timeEl.textContent = m.time;
            const actionBtn = this.el('div', 'moment-action-btn');
            actionBtn.textContent = '··';
            footer.append(timeEl, actionBtn);

            body.append(nameEl, textEl, footer);

            // 互动区域（点赞+评论）
            if (m.likes.length > 0 || m.comments.length > 0) {
                const interaction = this.el('div', 'moment-interaction');

                if (m.likes.length > 0) {
                    const likesDiv = this.el('div');
                    likesDiv.style.marginBottom = m.comments.length > 0 ? '4px' : '0';
                    const likesText = this.el('span', 'moment-likes-text');
                    likesText.textContent = '❤️ ' + m.likes.join('，');
                    likesDiv.appendChild(likesText);
                    interaction.appendChild(likesDiv);
                }

                if (m.comments.length > 0) {
                    const commentsDiv = this.el('div', 'moment-comments' + (m.likes.length > 0 ? ' with-border' : ''));
                    m.comments.forEach(c => {
                        const commentEl = this.el('div', 'moment-comment');
                        const nameSpan = this.el('span', 'moment-comment-name');
                        nameSpan.textContent = c.name;
                        commentEl.appendChild(nameSpan);
                        commentEl.appendChild(document.createTextNode('：' + c.text));
                        commentsDiv.appendChild(commentEl);
                    });
                    interaction.appendChild(commentsDiv);
                }

                body.appendChild(interaction);
            }

            header.append(avatar, body);
            item.appendChild(header);
            content.appendChild(item);
        });
    }

    // ============================================
    // Tab4: 我的页面
    // ============================================
    renderMe(content) {
        // 个人信息卡片
        const profileHtml = document.createElement('div');
        profileHtml.innerHTML =
            '<div class="me-profile-card">' +
            '  <div class="me-avatar">🐱</div>' +
            '  <div class="me-info">' +
            '    <div class="me-nickname">我的昵称</div>' +
            '    <div class="me-account">账号：WangWang_User</div>' +
            '  </div>' +
            '  <div class="me-qrcode">⊞ ›</div>' +
            '</div>' +
            '<div class="me-status-bar">' +
            '  <div class="me-status-btn">+ 状态</div>' +
            '  <div class="me-status-btn">👤👤👤 等40个朋友 ●</div>' +
            '</div>';
        content.appendChild(profileHtml);

        // 菜单分组
        const self = this;
        const menuGroups = [
            [{ icon: '✅', label: '服务', action() { self.currentView = 'service'; self.render(); } }],
            [
                { icon: '⭐', label: '收藏' },
                { icon: '🖼️', label: '朋友圈', action() { self.currentTab = 'moments'; self.render(); } },
                { icon: '📺', label: '视频号和公众号' },
                { icon: '🛒', label: '订单与卡包' },
                { icon: '😊', label: '表情' },
            ],
            [{ icon: '⚙️', label: '设置' }],
        ];

        menuGroups.forEach(group => {
            const section = this.el('div', 'me-menu-section');
            group.forEach(item => {
                const menuItem = this.el('div', 'me-menu-item');
                const iconEl = this.el('div', 'me-menu-icon');
                iconEl.textContent = item.icon;
                const labelEl = this.el('div', 'me-menu-label');
                labelEl.textContent = item.label;
                const arrowEl = this.el('div', 'me-menu-arrow');
                arrowEl.textContent = '›';
                menuItem.append(iconEl, labelEl, arrowEl);
                if (item.action) menuItem.onclick = item.action;
                section.appendChild(menuItem);
            });
            content.appendChild(section);
        });
    }

    // ============================================
    // 聊天详情页
    // ============================================
    renderChatDetail() {
        const conv = MOCK_CONVERSATIONS.find(c => c.id === this.currentChatId);
        const chatName = conv ? conv.name : '聊天';
        const messages = MOCK_CHAT_MESSAGES[this.currentChatId] || [
            { type: 'time', text: '今天 10:00' },
            { type: 'received', name: chatName, avatar: conv ? conv.avatar : '👤', text: '你好！' },
            { type: 'sent', text: '你好，有什么事吗？' },
        ];

        const isGroup = conv && (conv.name.includes('群') || messages.filter(m => m.type === 'received').map(m => m.name).filter((v, i, a) => a.indexOf(v) === i).length > 1);
        const memberCount = isGroup ? Math.floor(Math.random() * 40) + 10 : 0;
        const displayTitle = isGroup ? chatName + '(' + memberCount + ')' : chatName;

        const app = this.el('div', 'chat-app');

        // Header
        const header = this.el('div', 'chat-detail-header');
        const backBtn = this.el('div', 'chat-detail-back');
        backBtn.textContent = '‹';
        backBtn.onclick = () => { this.currentView = 'main'; this.currentChatId = null; this.render(); };
        const titleEl = this.el('div', 'chat-detail-title');
        titleEl.textContent = displayTitle;
        const moreBtn = this.el('div', 'chat-detail-more');
        moreBtn.textContent = '···';
        header.append(backBtn, titleEl, moreBtn);
        app.appendChild(header);

        // Messages area
        const msgArea = this.el('div', 'chat-messages');
        messages.forEach(msg => {
            if (msg.type === 'time') {
                const t = this.el('div', 'chat-msg-time');
                t.textContent = msg.text;
                msgArea.appendChild(t);
            } else if (msg.type === 'sent') {
                const row = this.el('div', 'chat-msg-row sent');
                const avatar = this.el('div', 'chat-msg-avatar');
                avatar.textContent = '🐱';
                const wrap = this.el('div', 'chat-msg-content-wrap');
                const bubble = this.el('div', 'chat-msg-bubble');
                bubble.textContent = msg.text;
                wrap.appendChild(bubble);
                row.append(avatar, wrap);
                msgArea.appendChild(row);
            } else if (msg.type === 'received') {
                const row = this.el('div', 'chat-msg-row received');
                const avatar = this.el('div', 'chat-msg-avatar');
                avatar.textContent = msg.avatar || '👤';
                const wrap = this.el('div', 'chat-msg-content-wrap');
                if (isGroup) {
                    const senderName = this.el('div', 'chat-msg-sender-name');
                    senderName.textContent = msg.name || chatName;
                    wrap.appendChild(senderName);
                }
                const bubble = this.el('div', 'chat-msg-bubble');
                bubble.textContent = msg.text;
                wrap.appendChild(bubble);
                row.append(avatar, wrap);
                msgArea.appendChild(row);
            }
        });
        app.appendChild(msgArea);

        // Input bar
        const inputBar = this.el('div', 'chat-input-bar');
        const voiceIcon = this.el('span', 'chat-input-icon');
        voiceIcon.textContent = '🎙️';
        const inputField = document.createElement('input');
        inputField.className = 'chat-input-field';
        inputField.type = 'text';
        inputField.placeholder = '';
        const emojiIcon = this.el('span', 'chat-input-icon');
        emojiIcon.textContent = '😊';
        const plusIcon = this.el('span', 'chat-input-icon');
        plusIcon.textContent = '⊕';

        const sendMsg = () => {
            const text = inputField.value.trim();
            if (!text) return;
            const row = this.el('div', 'chat-msg-row sent');
            const av = this.el('div', 'chat-msg-avatar');
            av.textContent = '🐱';
            const w = this.el('div', 'chat-msg-content-wrap');
            const b = this.el('div', 'chat-msg-bubble');
            b.textContent = text;
            w.appendChild(b);
            row.append(av, w);
            msgArea.appendChild(row);
            inputField.value = '';
            setTimeout(() => { msgArea.scrollTop = msgArea.scrollHeight; }, 50);
        };

        plusIcon.onclick = sendMsg;
        inputField.onkeydown = (e) => { if (e.key === 'Enter') sendMsg(); };

        inputBar.append(voiceIcon, inputField, emojiIcon, plusIcon);
        app.appendChild(inputBar);

        this.container.appendChild(app);
        setTimeout(() => { msgArea.scrollTop = msgArea.scrollHeight; }, 50);
    }

    // ============================================
    // 服务页
    // ============================================
    renderServicePage() {
        const app = this.el('div', 'chat-app');
        app.style.background = '#EDEDED';

        // Header
        const header = this.el('div', 'chat-detail-header');
        const backBtn = this.el('div', 'chat-detail-back');
        backBtn.textContent = '‹';
        backBtn.onclick = () => { this.currentView = 'main'; this.currentTab = 'me'; this.render(); };
        const titleEl = this.el('div', 'chat-detail-title');
        titleEl.textContent = '服务';
        const moreBtn = this.el('div', 'chat-detail-more');
        moreBtn.textContent = '···';
        header.append(backBtn, titleEl, moreBtn);
        app.appendChild(header);

        // Content
        const content = this.el('div', 'chat-content');
        content.style.position = 'relative';

        // 绿色卡片
        const card = this.el('div', 'service-green-card');

        const payItem = this.el('div', 'service-card-item');
        const payIcon = this.el('div', 'service-card-icon');
        payIcon.textContent = '⊡';
        const payLabel = this.el('div', 'service-card-label');
        payLabel.textContent = '收付款';
        payItem.append(payIcon, payLabel);

        const walletItem = this.el('div', 'service-card-item');
        const walletIcon = this.el('div', 'service-card-icon');
        walletIcon.textContent = '💳';
        const walletLabel = this.el('div', 'service-card-label');
        walletLabel.textContent = '钱包';
        const walletSub = this.el('div', 'service-card-sub');
        walletSub.textContent = '¥888.88';
        walletItem.append(walletIcon, walletLabel, walletSub);

        card.append(payItem, walletItem);
        content.appendChild(card);

        // 底部提示
        const footerTip = this.el('div', 'service-footer');
        footerTip.textContent = '所有服务已关闭，前往设置 ›';
        content.appendChild(footerTip);

        app.appendChild(content);
        this.container.appendChild(app);
    }

    // ============================================
    // 工具方法
    // ============================================
    el(tag, className) {
        const e = document.createElement(tag);
        if (className) e.className = className;
        return e;
    }
}