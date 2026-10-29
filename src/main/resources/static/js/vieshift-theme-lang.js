/**
 * VieShift - Theme (Light / Dark) & Multi-Language (i18n) Engine
 * Supported Languages:
 *   - vi: Tiếng Việt (Mặc định)
 *   - en: English
 *   - ja: 日本語
 *   - ko: 한국어
 *   - zh: 中文 (简体)
 */

(function () {
  'use strict';

  // ==========================================
  // 1. MULTI-LANGUAGE DICTIONARY
  // ==========================================
  const TRANSLATIONS = {
    vi: {
      lang_name: 'Tiếng Việt',
      lang_flag: '🇻🇳',
      theme_light_tip: 'Chuyển sang chế độ Tối',
      theme_dark_tip: 'Chuyển sang chế độ Sáng',
      nav_explore: 'Khám phá dịch vụ',
      nav_become_vieker: 'Trở thành Đối tác VieKer',
      nav_login: 'Đăng nhập',
      nav_register: 'Đăng ký Khách hàng',
      nav_my_jobs: 'Đơn của tôi',
      nav_create_job: 'Đặt thợ mới',
      nav_marketplace: 'Sàn nhận việc',
      nav_worker_jobs: 'Việc đang làm',
      nav_wallet: 'Ví thu nhập',
      nav_admin: 'Quản trị sàn',
      nav_logout: 'Đăng xuất',
      role_customer: 'Khách hàng',
      role_vieker: 'Đối tác VieKer',
      role_admin: 'Quản trị viên',

      // Hero & Homepage
      hero_title_1: 'Đặt thợ gia đình',
      hero_title_2: 'uy tín & tận tâm',
      hero_subtitle: 'Hơn 12.000+ thợ lành nghề đã qua kiểm duyệt lý lịch, phục vụ tức thì trong 30 phút.',
      hero_search_placeholder: 'Tìm kiếm dịch vụ: sửa điện nước, lắp máy lạnh, sơn tường, sửa khóa...',
      hero_search_btn: 'Tìm thợ ngay',
      popular_services_title: 'Dịch vụ được đặt nhiều nhất',
      popular_services_subtitle: 'Giá cả niêm yết rõ ràng · Không phát sinh chi phí vô lý',
      view_all_services: 'Xem tất cả dịch vụ',
      how_it_works_title: 'Quy trình 3 bước đơn giản',
      how_it_works_subtitle: 'Trải nghiệm đặt thợ gia đình nhanh chóng, minh bạch và an toàn tuyệt đối',
      step1_title: '1. Đăng yêu cầu',
      step1_desc: 'Mô tả công việc và mức giá mong muốn chỉ trong 1 phút.',
      step2_title: '2. Thợ nhận việc',
      step2_desc: 'Thợ lành nghề gần nhất được xác minh KYC sẽ liên hệ ngay.',
      step3_title: '3. Nghiệm thu & Thanh toán',
      step3_desc: 'Tiền được bảo lưu an toàn qua Ví Escrow. Chỉ giải ngân khi bạn hài lòng 100%.',
      guarantee_title: 'Cam kết Bảo đảm Hài lòng 100%',
      guarantee_desc: 'Số tiền đặt thợ được tạm giữ bảo đảm qua hệ thống Ví điện tử. Ban Quản Trị sẵn sàng can thiệp bảo vệ quyền lợi nếu công việc chưa đạt chuẩn.',

      // Customer Dashboard
      dash_title: 'Bàn làm việc Khách hàng',
      dash_hello: 'Xin chào',
      dash_subtitle: 'Quản lý các công việc đã đặt, theo dõi tiến độ thợ và lịch hẹn dịch vụ',
      tab_overview: 'Tổng quan',
      tab_create: 'Đăng yêu cầu mới',
      tab_jobs: 'Công việc của tôi',
      tab_wallet: 'Ví tiền & Lịch sử thanh toán',
      tab_profile: 'Thông tin tài khoản',
      stat_avail_balance: 'Số dư khả dụng',
      stat_held_balance: 'Đang bảo đảm đơn hàng',
      stat_active_jobs: 'Việc đang thực hiện',
      stat_completed_jobs: 'Việc đã hoàn thành',
      deposit_btn: 'Nạp tiền vào ví',
      withdraw_btn: 'Rút tiền',
      change_pwd_title: 'Đổi mật khẩu',

      // Footer
      footer_explore: 'Khám phá dịch vụ',
      footer_about: 'Về VieShift',
      footer_support: 'Hỗ trợ & Pháp lý',
      footer_download: 'Tải ứng dụng VieShift',
      footer_terms: 'Điều khoản sử dụng dịch vụ',
      footer_privacy: 'Chính sách bảo mật dữ liệu cá nhân',
      footer_escrow: 'Quy chế Thanh toán Tạm giữ (Escrow)',
      footer_kyc: 'Tiêu chuẩn Đối tác VieKer (KYC)',
      footer_dispute: 'Cơ chế Giải quyết Khiếu nại',
      footer_copyright: '© 2026 VieShift. Nền tảng Đặt thợ gia đình & Dịch vụ việc làm linh hoạt hàng đầu Việt Nam.'
    },

    en: {
      lang_name: 'English',
      lang_flag: '🇺🇸',
      theme_light_tip: 'Switch to Dark Mode',
      theme_dark_tip: 'Switch to Light Mode',
      nav_explore: 'Explore Services',
      nav_become_vieker: 'Become a VieKer Partner',
      nav_login: 'Log in',
      nav_register: 'Customer Register',
      nav_my_jobs: 'My Orders',
      nav_create_job: 'Book a Tasker',
      nav_marketplace: 'Job Marketplace',
      nav_worker_jobs: 'Active Jobs',
      nav_wallet: 'Earnings Wallet',
      nav_admin: 'Admin Console',
      nav_logout: 'Log out',
      role_customer: 'Customer',
      role_vieker: 'VieKer Partner',
      role_admin: 'Administrator',

      // Hero & Homepage
      hero_title_1: 'Book Home Taskers',
      hero_title_2: 'trusted & dedicated',
      hero_subtitle: 'Over 12,000+ background-verified skilled taskers ready to assist within 30 minutes.',
      hero_search_placeholder: 'Search services: plumbing, AC repair, painting, locksmith, cleaning...',
      hero_search_btn: 'Find Taskers',
      popular_services_title: 'Most Popular Services',
      popular_services_subtitle: 'Transparent upfront pricing · No hidden or unreasonable fees',
      view_all_services: 'View all services',
      how_it_works_title: 'Simple 3-Step Process',
      how_it_works_subtitle: 'Fast, transparent, and completely safe home maintenance experience',
      step1_title: '1. Post a Request',
      step1_desc: 'Describe your task and budget in just 1 minute.',
      step2_title: '2. Tasker Accepts',
      step2_desc: 'A verified local skilled tasker will contact you promptly.',
      step3_title: '3. Approve & Pay',
      step3_desc: 'Funds are securely held in escrow. Released only when you are 100% satisfied.',
      guarantee_title: '100% Satisfaction Guarantee',
      guarantee_desc: 'Payments safely held in escrow. Admin mediation guarantees your satisfaction.',

      // Customer Dashboard
      dash_title: 'Customer Dashboard',
      dash_hello: 'Welcome',
      dash_subtitle: 'Manage your booked jobs, track tasker progress, and review appointments',
      tab_overview: 'Overview',
      tab_create: 'Post a New Job',
      tab_jobs: 'My Jobs',
      tab_wallet: 'Wallet & Payment History',
      tab_profile: 'Account Settings',
      stat_avail_balance: 'Available Balance',
      stat_held_balance: 'Escrow Held Amount',
      stat_active_jobs: 'Jobs In Progress',
      stat_completed_jobs: 'Completed Jobs',
      deposit_btn: 'Deposit Funds',
      withdraw_btn: 'Withdraw Funds',
      change_pwd_title: 'Change Password',

      // Footer
      footer_explore: 'Explore Services',
      footer_about: 'About VieShift',
      footer_support: 'Support & Legal',
      footer_download: 'Download VieShift App',
      footer_terms: 'Terms of Service',
      footer_privacy: 'Privacy Policy',
      footer_escrow: 'Escrow Regulations',
      footer_kyc: 'VieKer Partner Standards (KYC)',
      footer_dispute: 'Dispute Resolution Mechanism',
      footer_copyright: '© 2026 VieShift. Vietnam\'s Leading Home Tasker & Flexible Work Platform.'
    },

    ja: {
      lang_name: '日本語',
      lang_flag: '🇯🇵',
      theme_light_tip: 'ダークモードに切替',
      theme_dark_tip: 'ライトモードに切替',
      nav_explore: 'サービスを探す',
      nav_become_vieker: 'パートナー職人になる',
      nav_login: 'ログイン',
      nav_register: 'お客様登録',
      nav_my_jobs: '依頼一覧',
      nav_create_job: '職人を依頼',
      nav_marketplace: '仕事一覧',
      nav_worker_jobs: '進行中の仕事',
      nav_wallet: '報酬ウォレット',
      nav_admin: '管理パネル',
      nav_logout: 'ログアウト',
      role_customer: 'お客様',
      role_vieker: 'パートナー職人',
      role_admin: '管理者',

      // Hero & Homepage
      hero_title_1: '家庭向け専門職人を依頼',
      hero_title_2: '安心・信頼のクオリティ',
      hero_subtitle: '12,000人以上の身元確認済み職人が、30分以内に駆けつけます。',
      hero_search_placeholder: 'サービスを検索：水道修理、エアコン清掃、壁面塗装、鍵交換...',
      hero_search_btn: '職人を検索',
      popular_services_title: '人気のサービス一覧',
      popular_services_subtitle: '明朗会計 · 不当な追加費用は一切ありません',
      view_all_services: 'すべてのサービスを見る',
      how_it_works_title: 'カンタン3ステップ',
      how_it_works_subtitle: 'スピーディーで透明性が高く、安全な出張サービス体験',
      step1_title: '1. 依頼内容を投稿',
      step1_desc: '依頼内容と希望予算を1分で入力・投稿できます。',
      step2_title: '2. 職人が受注',
      step2_desc: '近隣の厳選されたKYC認証済み職人が迅速に対応します。',
      step3_title: '3. 検収・お支払い',
      step3_desc: '代金は安全に仮保持。仕上がりに100%満足した段階でのみ支払いが完了します。',
      guarantee_title: '100% 満足保証コミットメント',
      guarantee_desc: '安心のエスクロー決済保護と24時間体制のサポート。',

      // Customer Dashboard
      dash_title: 'お客様ダッシュボード',
      dash_hello: 'こんにちは',
      dash_subtitle: '依頼した仕事の管理、職人の進捗追跡、予約履歴の確認',
      tab_overview: '概要',
      tab_create: '新規依頼を投稿',
      tab_jobs: '自分の依頼',
      tab_wallet: 'ウォレット・決済履歴',
      tab_profile: 'アカウント情報',
      stat_avail_balance: '利用可能残高',
      stat_held_balance: '仮保持中（エスクロー）',
      stat_active_jobs: '進行中の依頼',
      stat_completed_jobs: '完了した依頼',
      deposit_btn: 'チャージする',
      withdraw_btn: '出金申請',
      change_pwd_title: 'パスワード変更',

      // Footer
      footer_explore: 'サービス一覧',
      footer_about: 'VieShiftについて',
      footer_support: 'サポート＆利用規約',
      footer_download: 'アプリをダウンロード',
      footer_terms: '利用規約',
      footer_privacy: 'プライバシーポリシー',
      footer_escrow: '仮払い（エスクロー）規約',
      footer_kyc: '職人パートナー基準（KYC）',
      footer_dispute: '苦情・紛争解決手続き',
      footer_copyright: '© 2026 VieShift. ベトナムをリードする家庭向け出張職人プラットフォーム。'
    },

    ko: {
      lang_name: '한국어',
      lang_flag: '🇰🇷',
      theme_light_tip: '다크 모드로 전환',
      theme_dark_tip: '라이트 모드로 전환',
      nav_explore: '서비스 탐색',
      nav_become_vieker: '파트너 전문가 등록',
      nav_login: '로그인',
      nav_register: '고객 회원가입',
      nav_my_jobs: '내 주문',
      nav_create_job: '전문가 예약',
      nav_marketplace: '작업 매칭 센터',
      nav_worker_jobs: '진행 중인 작업',
      nav_wallet: '수익 지갑',
      nav_admin: '관리 센터',
      nav_logout: '로그아웃',
      role_customer: '고객',
      role_vieker: '전문가 파트너',
      role_admin: '시스템 관리자',

      // Hero & Homepage
      hero_title_1: '가정 전문가 예약',
      hero_title_2: '신뢰와 정성을 담아',
      hero_subtitle: '신원 검증을 통과한 12,000명 이상의 숙련 전문가가 30분 내 방문 준비.',
      hero_search_placeholder: '서비스 검색: 배관 수리, 에어컨 청소, 벽면 도색, 열쇠 수리...',
      hero_search_btn: '전문가 찾기',
      popular_services_title: '가장 인기 있는 서비스',
      popular_services_subtitle: '투명하고 명확한 정찰 가격제 · 부당한 추가 요금 없음',
      view_all_services: '모든 서비스 보기',
      how_it_works_title: '간편한 3단계 진행',
      how_it_works_subtitle: '빠르고 투명하며 완벽하게 안전한 가정 서비스 경험',
      step1_title: '1. 요청 등록',
      step1_desc: '1분 만에 작업 내용과 원하는 예산을 등록하세요.',
      step2_title: '2. 전문가 접수',
      step2_desc: '인증된 가까운 전문 기술자가 신속히 연락드립니다.',
      step3_title: '3. 검수 및 정산',
      step3_desc: '안전 에스크로 보관. 작업에 100% 만족할 때만 대금이 지급됩니다.',
      guarantee_title: '100% 만족 보장 약속',
      guarantee_desc: '안전한 에스크로 결제 보호 및 24시간 분쟁 처리 지원.',

      // Customer Dashboard
      dash_title: '고객 대시보드',
      dash_hello: '안녕하세요',
      dash_subtitle: '예약한 작업 관리, 전문가 진행 상황 추적 및 일정 확인',
      tab_overview: '개요',
      tab_create: '새 작업 요청',
      tab_jobs: '내 작업 목록',
      tab_wallet: '지갑 및 결제 내역',
      tab_profile: '계정 설정',
      stat_avail_balance: '사용 가능 잔액',
      stat_held_balance: '에스크로 보관액',
      stat_active_jobs: '진행 중인 작업',
      stat_completed_jobs: '완료된 작업',
      deposit_btn: '지갑 충전',
      withdraw_btn: '출금 요청',
      change_pwd_title: '비밀번호 변경',

      // Footer
      footer_explore: '서비스 탐색',
      footer_about: 'VieShift 소개',
      footer_support: '고객지원 및 약관',
      footer_download: '앱 다운로드',
      footer_terms: '서비스 이용약관',
      footer_privacy: '개인정보처리방침',
      footer_escrow: '에스크로 규정',
      footer_kyc: '전문가 파트너 기준 (KYC)',
      footer_dispute: '이의 제기 및 분쟁 해결 기준',
      footer_copyright: '© 2026 VieShift. 베트남 최고의 홈 테스커 및 유연 일자리 플랫폼.'
    },

    zh: {
      lang_name: '中文 (简体)',
      lang_flag: '🇨🇳',
      theme_light_tip: '切换至深色模式',
      theme_dark_tip: '切换至浅色模式',
      nav_explore: '探索服务',
      nav_become_vieker: '成为服务师傅',
      nav_login: '登录',
      nav_register: '用户注册',
      nav_my_jobs: '我的订单',
      nav_create_job: '预约师傅',
      nav_marketplace: '接单广场',
      nav_worker_jobs: '进行中的工作',
      nav_wallet: '收益钱包',
      nav_admin: '管理后台',
      nav_logout: '退出登录',
      role_customer: '客户',
      role_vieker: '服务师傅',
      role_admin: '管理员',

      // Hero & Homepage
      hero_title_1: '预约家庭专业师傅',
      hero_title_2: '值得信赖 · 贴心专业',
      hero_subtitle: '超过12,000+严格实名认证的专业师傅，30分钟内快速响应。',
      hero_search_placeholder: '搜索服务：水电维修、空调清洗、墙面刷新、开锁换锁...',
      hero_search_btn: '立即找师傅',
      popular_services_title: '热门服务项目',
      popular_services_subtitle: '透明标价 · 绝无不合理隐形增费',
      view_all_services: '查看所有服务',
      how_it_works_title: '简单3步轻松搞定',
      how_it_works_subtitle: '快速、透明、绝对安全的家庭维修服务体验',
      step1_title: '1. 发布需求',
      step1_desc: '只需1分钟，填写您的服务需求与期望预算。',
      step2_title: '2. 师傅接单',
      step2_desc: '附近严格认证的专业师傅将快速与您联系。',
      step3_title: '3. 验收支付',
      step3_desc: '资金全程托管保障，确认100%满意后再解冻结账。',
      guarantee_title: '100% 满意度保障承诺',
      guarantee_desc: '担保交易资金安全保护与24小时客服争议协助。',

      // Customer Dashboard
      dash_title: '客户工作台',
      dash_hello: '您好',
      dash_subtitle: '管理您的预约订单、追踪师傅进度及服务日程',
      tab_overview: '总览',
      tab_create: '发布新需求',
      tab_jobs: '我的订单',
      tab_wallet: '钱包与支付记录',
      tab_profile: '账户设置',
      balance_avail: '可用余额',
      balance_held: '托管保障金额',
      jobs_active: '进行中订单',
      jobs_completed: '已完成订单',
      deposit_btn: '充值钱包',
      withdraw_btn: '申请提现',
      change_pwd_title: '修改密码',

      // Footer
      footer_explore: '探索服务',
      footer_about: '关于 VieShift',
      footer_support: '帮助与条款',
      footer_download: '下载移动端 App',
      footer_terms: '服务条款',
      footer_privacy: '隐私政策',
      footer_escrow: '托管资金保障准则',
      footer_kyc: '服务师傅认证标准 (KYC)',
      footer_dispute: '争议投诉处理机制',
      footer_copyright: '© 2026 VieShift. 越南领先的家庭上门服务与灵活就业平台。'
    }
  };

  // ==========================================
  // 2. THEME ENGINE (LIGHT / DARK)
  // ==========================================
  function getCurrentTheme() {
    const saved = localStorage.getItem('vieshift_theme');
    if (saved) return saved;
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      return 'dark';
    }
    return 'light';
  }

  function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('vieshift_theme', theme);

    const toggleBtn = document.getElementById('themeToggleBtn');
    if (!toggleBtn) return;

    const darkIcon = toggleBtn.querySelector('.theme-icon-dark');
    const lightIcon = toggleBtn.querySelector('.theme-icon-light');
    const currentLang = getCurrentLanguage();
    const t = TRANSLATIONS[currentLang] || TRANSLATIONS.vi;

    if (theme === 'dark') {
      if (darkIcon) darkIcon.style.display = 'none';
      if (lightIcon) lightIcon.style.display = 'inline-block';
      toggleBtn.setAttribute('title', t.theme_dark_tip);
    } else {
      if (darkIcon) darkIcon.style.display = 'inline-block';
      if (lightIcon) lightIcon.style.display = 'none';
      toggleBtn.setAttribute('title', t.theme_light_tip);
    }
  }

  function toggleTheme() {
    const current = document.documentElement.getAttribute('data-theme') || getCurrentTheme();
    const next = current === 'dark' ? 'light' : 'dark';
    applyTheme(next);
  }

  // ==========================================
  // 3. MULTI-LANGUAGE ENGINE
  // ==========================================
  function getCurrentLanguage() {
    const saved = localStorage.getItem('vieshift_lang');
    if (saved && TRANSLATIONS[saved]) return saved;
    return 'vi';
  }

  function applyLanguage(lang) {
    if (!TRANSLATIONS[lang]) lang = 'vi';
    localStorage.setItem('vieshift_lang', lang);
    document.documentElement.setAttribute('lang', lang);

    const t = TRANSLATIONS[lang];

    // 1. Cập nhật nhãn nút Selector
    const flagEl = document.getElementById('currentLangFlag');
    const labelEl = document.getElementById('currentLangLabel');
    if (flagEl) flagEl.textContent = t.lang_flag;
    if (labelEl) labelEl.textContent = t.lang_name;

    // 2. Đánh dấu active trong menu dropdown
    document.querySelectorAll('.lang-option').forEach(opt => {
      const optLang = opt.getAttribute('data-lang');
      if (optLang === lang) {
        opt.classList.add('active');
      } else {
        opt.classList.remove('active');
      }
    });

    // 3. Cập nhật tooltip theme button
    const currentTheme = document.documentElement.getAttribute('data-theme') || getCurrentTheme();
    const toggleBtn = document.getElementById('themeToggleBtn');
    if (toggleBtn) {
      toggleBtn.setAttribute('title', currentTheme === 'dark' ? t.theme_dark_tip : t.theme_light_tip);
    }

    // 4. Quét và dịch tất cả phần tử có data-i18n
    document.querySelectorAll('[data-i18n]').forEach(el => {
      const key = el.getAttribute('data-i18n');
      if (t[key]) {
        if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
          el.setAttribute('placeholder', t[key]);
        } else {
          el.textContent = t[key];
        }
      }
    });

    // 5. Tự động dịch các text nodes tiêu biểu trong Navbar & Footer
    translateCommonSelectors(t);

    // 6. Tích hợp hỗ trợ Google Translate nếu người dùng chọn ngoại ngữ khác tiếng Việt
    triggerGoogleTranslate(lang);
  }

  function translateCommonSelectors(t) {
    // Menu điều hướng
    const selectorMap = [
      { sel: '.tr-navbar a[href*="/explore"]', text: t.nav_explore },
      { sel: '.tr-navbar a[href*="/become-vieker"]', text: t.nav_become_vieker },
      { sel: '.tr-navbar a[href*="/login"]', text: t.nav_login },
      { sel: '.tr-navbar a[href*="/register"]', text: t.nav_register },
      { sel: '.tr-navbar a[href*="customer?tab=jobs"]', text: t.nav_my_jobs },
      { sel: '.tr-navbar a[href*="customer?tab=create"]', text: t.nav_create_job },
      { sel: '.tr-navbar a[href*="vieker?tab=marketplace"]', text: t.nav_marketplace },
      { sel: '.tr-navbar a[href*="vieker?tab=my-jobs"]', text: t.nav_worker_jobs },
      { sel: '.tr-navbar a[href*="vieker?tab=wallet"]', text: t.nav_wallet },
      { sel: '.tr-navbar a[href*="/admin"]', text: t.nav_admin },

      // User tags
      { sel: '.user-role-tag.role-customer', text: t.role_customer },
      { sel: '.user-role-tag.role-vieker', text: t.role_vieker },
      { sel: '.user-role-tag.role-admin', text: t.role_admin },

      // Hero search
      { sel: 'input[name="q"], input[name="keyword"], .tr-search-input', placeholder: t.hero_search_placeholder },
      { sel: 'button[type="submit"].btn-primary, .tr-search-btn', title: t.hero_search_btn },

      // Footer
      { sel: '.tr-footer-col:nth-child(1) h4', text: t.footer_explore },
      { sel: '.tr-footer-col:nth-child(2) h4', text: t.footer_about },
      { sel: '.tr-footer-col:nth-child(3) h4', text: t.footer_support },
      { sel: '.tr-footer-col:nth-child(4) h4', text: t.footer_download },
      { sel: '.tr-footer-links a[href*="/terms"]', text: t.footer_terms },
      { sel: '.tr-footer-links a[href*="/privacy"]', text: t.footer_privacy },
      { sel: '.tr-footer-links a[href*="/terms#escrow"]', text: t.footer_escrow },
      { sel: '.tr-footer-links a[href*="/terms#vieker"]', text: t.footer_kyc },
      { sel: '.tr-footer-links a[href*="/terms#dispute"]', text: t.footer_dispute }
    ];

    selectorMap.forEach(item => {
      document.querySelectorAll(item.sel).forEach(el => {
        if (item.condition && !item.condition(el)) return;
        if (item.text) {
          // Bảo lưu icon nếu có bên trong thẻ
          const icon = el.querySelector('i');
          if (icon) {
            el.innerHTML = '';
            el.appendChild(icon);
            el.appendChild(document.createTextNode(' ' + item.text));
          } else {
            el.textContent = item.text;
          }
        }
        if (item.placeholder) {
          el.setAttribute('placeholder', item.placeholder);
        }
      });
    });
  }

  // Google Translate Helper for full paragraph auto-translation
  function triggerGoogleTranslate(lang) {
    if (lang === 'vi') {
      // Xóa cookie dịch nếu quay lại Tiếng Việt
      document.cookie = 'googtrans=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
      document.cookie = 'googtrans=; expires=Thu, 01 Jan 1970 00:00:00 UTC; domain=' + window.location.hostname + '; path=/;';
      const iframe = document.querySelector('iframe.goog-te-banner-frame');
      if (iframe) iframe.style.display = 'none';
      return;
    }

    const gtLang = lang === 'zh' ? 'zh-CN' : lang;
    document.cookie = `googtrans=/vi/${gtLang}; path=/;`;
    document.cookie = `googtrans=/vi/${gtLang}; domain=${window.location.hostname}; path=/;`;

    // Nạp script Google Translate nếu chưa có
    if (!window.google || !window.google.translate) {
      if (!document.getElementById('google-translate-script')) {
        const s = document.createElement('script');
        s.id = 'google-translate-script';
        s.src = '//translate.google.com/translate_a/element.js?cb=vieshiftGoogleTranslateCallback';
        document.body.appendChild(s);
      }
    } else {
      // Trigger combo change nếu widget đã sẵn sàng
      const select = document.querySelector('select.goog-te-combo');
      if (select) {
        select.value = gtLang;
        select.dispatchEvent(new Event('change'));
      }
    }
  }

  window.vieshiftGoogleTranslateCallback = function () {
    if (window.google && window.google.translate) {
      new window.google.translate.TranslateElement({
        pageLanguage: 'vi',
        includedLanguages: 'vi,en,ja,ko,zh-CN',
        autoDisplay: false
      }, 'vieshift_google_translate_hidden');
    }
  };

  // ==========================================
  // 4. EVENT LISTENERS & DOM READY
  // ==========================================
  document.addEventListener('DOMContentLoaded', function () {
    // 1. Áp dụng Theme
    applyTheme(getCurrentTheme());

    const toggleBtn = document.getElementById('themeToggleBtn');
    if (toggleBtn) {
      toggleBtn.addEventListener('click', toggleTheme);
    }

    // 2. Dropdown Ngôn ngữ
    const langBtn = document.getElementById('langDropdownBtn');
    const langMenu = document.getElementById('langDropdownMenu');

    if (langBtn && langMenu) {
      langBtn.addEventListener('click', function (e) {
        e.stopPropagation();
        const isOpen = langMenu.style.display === 'block';
        langMenu.style.display = isOpen ? 'none' : 'block';
        langBtn.setAttribute('aria-expanded', !isOpen);
      });

      document.querySelectorAll('.lang-option').forEach(opt => {
        opt.addEventListener('click', function () {
          const selectedLang = this.getAttribute('data-lang');
          applyLanguage(selectedLang);
          langMenu.style.display = 'none';
          if (langBtn) langBtn.setAttribute('aria-expanded', 'false');
        });
      });

      // Đóng menu khi click ra ngoài
      document.addEventListener('click', function (e) {
        if (!langMenu.contains(e.target) && e.target !== langBtn) {
          langMenu.style.display = 'none';
          if (langBtn) langBtn.setAttribute('aria-expanded', 'false');
        }
      });
    }

    // 3. Áp dụng Ngôn ngữ đã lưu
    applyLanguage(getCurrentLanguage());

    // 4. Khởi động Chuông Thông Báo (Notification Center)
    initNotificationCenter();
  });

  // ==========================================
  // 5. NOTIFICATION CENTER ENGINE
  // ==========================================
  let lastUnreadCount = 0;

  function initNotificationCenter() {
    const bellBtn = document.getElementById('notifBellBtn');
    const menu = document.getElementById('notifDropdownMenu');

    if (bellBtn && menu) {
      bellBtn.addEventListener('click', function (e) {
        e.stopPropagation();
        const isOpen = menu.style.display === 'block';
        menu.style.display = isOpen ? 'none' : 'block';
        if (!isOpen) {
          fetchNotifications();
        }
      });

      document.addEventListener('click', function (e) {
        if (!menu.contains(e.target) && e.target !== bellBtn) {
          menu.style.display = 'none';
        }
      });

      fetchNotifications();
      setInterval(fetchNotifications, 12000);
    }
  }

  function fetchNotifications() {
    fetch('/api/notifications?limit=8')
      .then(res => {
        if (res.status === 401 || res.status === 403) return null;
        return res.json();
      })
      .then(data => {
        if (!data) return;
        renderNotifications(data);
      })
      .catch(err => {});
  }

  function renderNotifications(data) {
    const badge = document.getElementById('notifUnreadBadge');
    const container = document.getElementById('notifListContainer');
    const unreadCount = data.unreadCount || 0;

    if (badge) {
      if (unreadCount > 0) {
        badge.textContent = unreadCount > 9 ? '9+' : unreadCount;
        badge.style.display = 'inline-flex';

        // Hiệu ứng rung nhẹ khi có thông báo mới
        if (unreadCount > lastUnreadCount) {
          const bell = document.getElementById('notifBellBtn');
          if (bell) {
            bell.classList.add('bell-ringing');
            setTimeout(() => bell.classList.remove('bell-ringing'), 800);
          }
        }
      } else {
        badge.style.display = 'none';
      }
    }
    lastUnreadCount = unreadCount;

    if (container && data.notifications) {
      if (data.notifications.length === 0) {
        container.innerHTML = '<div style="text-align: center; padding: 28px 10px; color: var(--text-muted); font-size: 0.85rem;"><i class="bi bi-bell-slash" style="font-size: 1.6rem; display: block; margin-bottom: 6px; color: var(--border-strong);"></i>Bạn chưa có thông báo nào</div>';
        return;
      }

      container.innerHTML = '';
      data.notifications.forEach(n => {
        const item = document.createElement('a');
        item.href = n.link || '#';
        item.className = 'notif-item' + (n.isRead ? '' : ' unread');
        item.style.display = 'flex';
        item.style.alignItems = 'flex-start';
        item.style.gap = '10px';
        item.style.padding = '10px 14px';
        item.style.borderBottom = '1px solid var(--border)';
        item.style.textDecoration = 'none';
        item.style.color = 'inherit';
        item.style.transition = 'background var(--transition-fast)';

        item.onclick = function () {
          if (!n.isRead) {
            fetch('/api/notifications/' + n.id + '/read', { method: 'POST' }).catch(() => {});
          }
        };

        const iconWrap = document.createElement('div');
        iconWrap.style.width = '34px';
        iconWrap.style.height = '34px';
        iconWrap.style.borderRadius = '50%';
        iconWrap.style.background = (n.color || '#C24D63') + '1A';
        iconWrap.style.color = n.color || '#C24D63';
        iconWrap.style.display = 'flex';
        iconWrap.style.alignItems = 'center';
        iconWrap.style.justifyContent = 'center';
        iconWrap.style.fontSize = '1.05rem';
        iconWrap.style.flexShrink = '0';
        iconWrap.innerHTML = `<i class="bi ${n.icon || 'bi-bell'}"></i>`;

        const body = document.createElement('div');
        body.style.flex = '1';
        body.style.minWidth = '0';

        const title = document.createElement('div');
        title.style.fontWeight = n.isRead ? '600' : '800';
        title.style.fontSize = '0.86rem';
        title.style.color = 'var(--text-main)';
        title.style.marginBottom = '2px';
        title.textContent = n.title;

        const content = document.createElement('div');
        content.style.fontSize = '0.78rem';
        content.style.color = 'var(--text-muted)';
        content.style.lineHeight = '1.35';
        content.style.wordBreak = 'break-word';
        content.textContent = n.content;

        const time = document.createElement('div');
        time.style.fontSize = '0.7rem';
        time.style.color = 'var(--text-light)';
        time.style.marginTop = '4px';
        time.textContent = n.createdAt;

        body.appendChild(title);
        body.appendChild(content);
        body.appendChild(time);

        item.appendChild(iconWrap);
        item.appendChild(body);

        container.appendChild(item);
      });
    }
  }

  window.markAllNotificationsRead = function () {
    fetch('/api/notifications/read-all', { method: 'POST' })
      .then(() => fetchNotifications())
      .catch(() => {});
  };

})();
