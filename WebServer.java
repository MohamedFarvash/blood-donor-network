import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebServer {

    static final String[] BLOOD_GROUPS = {"O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-"};

    static final String[][] COMPAT = {
        {"O-", "All groups", "O-"},
        {"O+", "O+, A+, B+, AB+", "O+, O-"},
        {"A-", "A-, A+, AB-, AB+", "A-, O-"},
        {"A+", "A+, AB+", "A+, A-, O+, O-"},
        {"B-", "B-, B+, AB-, AB+", "B-, O-"},
        {"B+", "B+, AB+", "B+, B-, O+, O-"},
        {"AB-", "AB-, AB+", "AB-, A-, B-, O-"},
        {"AB+", "AB+", "All groups"}
    };

    static final String CSS = """
        :root{--crimson:#e11d48;--wine:#9f1239;--plum:#6d1a4f;--indigo:#1e1b4b;--rose:#fb7185;--teal:#0d9488;--ink:#1e1b2e;--muted:#6b6880;--line:#f0e4e8;--bg:#fff8f6}
        *{box-sizing:border-box}
        html{scroll-behavior:smooth}
        body{margin:0;font-family:'Inter','Segoe UI',Arial,sans-serif;color:var(--ink);background-color:var(--bg);
        background-image:radial-gradient(900px 520px at 6% -8%,#ffe0e6 0,transparent 60%),radial-gradient(800px 520px at 100% 0,#e9e1ff 0,transparent 55%),radial-gradient(700px 520px at 50% 115%,#ffe9d6 0,transparent 60%);
        background-attachment:fixed}
        a{color:inherit}
        nav{position:sticky;top:0;z-index:20;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px;
        padding:12px 5%;background:rgba(255,255,255,.78);backdrop-filter:blur(14px);border-bottom:1px solid var(--line)}
        .brand{display:flex;align-items:center;gap:10px;font-weight:800;font-size:1.15rem;text-decoration:none}
        .logo{width:40px;height:40px;border-radius:13px;display:grid;place-items:center;font-size:1.25rem;
        background:linear-gradient(135deg,#fb7185,#be123c);box-shadow:0 8px 18px rgba(225,29,72,.35);animation:beat 2.4s ease-in-out infinite}
        @keyframes beat{0%,48%,100%{transform:scale(1)}12%{transform:scale(1.12)}24%{transform:scale(1)}36%{transform:scale(1.08)}}
        .links{display:flex;gap:4px;flex-wrap:wrap}
        .links a{text-decoration:none;color:var(--muted);font-weight:600;padding:9px 15px;border-radius:999px;transition:.2s}
        .links a:hover{color:var(--crimson);background:#fff1f3;transform:translateY(-1px)}
        .links a.active{color:#fff;background:linear-gradient(135deg,#f43f5e,#be123c);box-shadow:0 8px 16px rgba(225,29,72,.3)}
        main{max-width:1080px;margin:30px auto 50px;padding:0 20px}

        .hero{position:relative;overflow:hidden;display:grid;grid-template-columns:1.25fr 1fr;gap:30px;align-items:center;
        padding:58px 48px;border-radius:30px;color:#fff;
        background:linear-gradient(135deg,#1e1b4b 0%,#6d1a4f 48%,#e11d48 100%);box-shadow:0 28px 64px rgba(109,26,79,.38)}
        .hero>*{position:relative;z-index:1}
        .blob{position:absolute!important;z-index:0!important;border-radius:50%;filter:blur(46px);opacity:.55;animation:drift 12s ease-in-out infinite}
        .b1{width:300px;height:300px;background:#fb7185;top:-90px;right:-60px}
        .b2{width:260px;height:260px;background:#7c3aed;bottom:-100px;left:-60px;animation-delay:-5s}
        @keyframes drift{0%,100%{transform:translate(0,0)}50%{transform:translate(28px,-22px)}}
        .tag{display:inline-block;background:rgba(255,255,255,.14);border:1px solid rgba(255,255,255,.28);padding:7px 15px;border-radius:999px;font-size:.85rem;font-weight:600;margin-bottom:18px}
        .hero h1{font-size:clamp(2rem,5vw,3.4rem);line-height:1.1;margin:0 0 14px;font-weight:800}
        .grad{background:linear-gradient(90deg,#fecdd3,#fde68a);-webkit-background-clip:text;background-clip:text;color:transparent}
        .hero p{color:#ece9f7;font-size:1.1rem;line-height:1.65;margin:0 0 26px;max-width:520px}
        .ecg{width:260px;height:60px;margin-top:26px}
        .ecg path{fill:none;stroke:#fecdd3;stroke-width:3;stroke-linecap:round;stroke-linejoin:round;stroke-dasharray:420;stroke-dashoffset:420;animation:ecg 3.2s linear infinite}
        @keyframes ecg{0%{stroke-dashoffset:420}60%,100%{stroke-dashoffset:0}}
        .drop-wrap{position:relative;display:grid;place-items:center;min-height:280px}
        .drop{width:190px;height:190px;border-radius:50%;display:grid;place-items:center;font-size:5.4rem;
        background:radial-gradient(circle,rgba(251,113,133,.6),rgba(251,113,133,.06) 70%);animation:float 3.6s ease-in-out infinite}
        @keyframes float{0%,100%{transform:translateY(0)}50%{transform:translateY(-14px)}}
        .ring{position:absolute;width:190px;height:190px;border-radius:50%;border:2px solid rgba(255,255,255,.35);animation:ripple 3.6s ease-out infinite}
        .r2{animation-delay:1.8s}
        @keyframes ripple{0%{transform:scale(.9);opacity:.8}100%{transform:scale(1.9);opacity:0}}
        .fchip{position:absolute;padding:7px 14px;border-radius:999px;font-weight:800;font-size:.9rem;color:#fff;
        background:rgba(255,255,255,.16);border:1px solid rgba(255,255,255,.35);backdrop-filter:blur(6px);animation:bob 4.2s ease-in-out infinite;animation-delay:var(--d,0s)}
        @keyframes bob{0%,100%{transform:translateY(0)}50%{transform:translateY(-10px)}}
        @media(max-width:800px){.hero{grid-template-columns:1fr;padding:38px 26px}.drop-wrap{display:none}}

        .actions{display:flex;gap:14px;flex-wrap:wrap}
        .btn{position:relative;overflow:hidden;display:inline-flex;align-items:center;gap:8px;border:0;cursor:pointer;text-decoration:none;
        font:inherit;font-weight:700;padding:13px 26px;border-radius:999px;color:#fff;
        background:linear-gradient(135deg,#f43f5e,#be123c);box-shadow:0 10px 22px rgba(225,29,72,.35);transition:transform .15s,box-shadow .15s}
        .btn::after{content:'';position:absolute;inset:0;background:linear-gradient(120deg,transparent 30%,rgba(255,255,255,.38) 50%,transparent 70%);transform:translateX(-120%);transition:transform .6s}
        .btn:hover{transform:translateY(-2px);box-shadow:0 14px 28px rgba(225,29,72,.45)}
        .btn:hover::after{transform:translateX(120%)}
        .btn.ghost{background:rgba(255,255,255,.14);border:1px solid rgba(255,255,255,.4);box-shadow:none}
        .btn.outline{background:#fff;color:var(--crimson);border:2px solid #fecdd3;box-shadow:none}
        .btn.small{padding:8px 16px;font-size:.88rem}

        .card{background:#fff;border:1px solid var(--line);border-radius:22px;padding:28px;box-shadow:0 10px 28px rgba(80,30,60,.07);transition:transform .25s,box-shadow .25s}
        .card.lift:hover{transform:translateY(-5px);box-shadow:0 18px 38px rgba(225,29,72,.16)}
        .chip{width:54px;height:54px;border-radius:17px;display:grid;place-items:center;font-size:1.6rem;flex:none;
        background:linear-gradient(135deg,#fff1f3,#ffe4ea);box-shadow:inset 0 0 0 1px #fecdd3;
        animation:pop .6s cubic-bezier(.34,1.56,.64,1) backwards;animation-delay:var(--d,0s);transition:transform .25s}
        .card:hover .chip{transform:scale(1.14) rotate(-7deg)}
        @keyframes pop{0%{transform:scale(0) rotate(-25deg);opacity:0}100%{transform:scale(1) rotate(0);opacity:1}}
        .js .reveal{opacity:0;transform:translateY(24px);transition:opacity .7s ease,transform .7s ease;transition-delay:var(--d,0s)}
        .js .reveal.in{opacity:1;transform:none}

        .stats{display:grid;grid-template-columns:repeat(auto-fit,minmax(230px,1fr));gap:20px;margin:26px 0}
        .stat{display:flex;align-items:center;gap:16px}
        .stat .num{font-size:2.3rem;font-weight:800;line-height:1;background:linear-gradient(135deg,#be123c,#6d1a4f);-webkit-background-clip:text;background-clip:text;color:transparent}
        .stat .label{color:var(--muted);font-weight:600;margin-top:4px}
        h2{margin:0 0 4px;font-size:1.5rem}
        .sub{color:var(--muted);margin:0 0 20px;line-height:1.5}
        .head{display:flex;align-items:center;gap:16px;margin-bottom:8px}
        .head .sub{margin:0}
        .section-title{text-align:center;margin:50px 0 24px;font-size:1.8rem;font-weight:800}
        .section-title small{display:block;color:var(--crimson);font-size:.8rem;letter-spacing:.14em;text-transform:uppercase;margin-bottom:6px}
        .grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(230px,1fr));gap:20px}
        .step .n{position:absolute;top:18px;right:22px;font-size:2.6rem;font-weight:800;color:#ffe4ea}
        .step{position:relative}
        .step h3,.feat h3{margin:14px 0 6px}
        .step p,.feat p{margin:0;color:var(--muted);line-height:1.55}
        .cta{margin-top:44px;text-align:center;border-radius:26px;padding:42px 24px;color:#fff;
        background:linear-gradient(135deg,#6d1a4f,#be123c 60%,#f97316);box-shadow:0 20px 44px rgba(190,18,60,.3)}
        .cta .sub{color:#ffe4e6}
        .cta .btn{margin:4px}
        .cta .btn.outline{border-color:transparent}

        label.l{display:block;font-weight:700;font-size:.9rem;margin:18px 0 8px}
        .in{width:100%;padding:13px 16px;border:2px solid var(--line);border-radius:14px;font:inherit;background:#fff;transition:.2s}
        .in:focus{outline:none;border-color:#fb7185;box-shadow:0 0 0 4px rgba(251,113,133,.2)}
        .row{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:18px}
        .pills{display:flex;flex-wrap:wrap;gap:10px}
        .pill{position:relative}
        .pill input{position:absolute;opacity:0;width:100%;height:100%;left:0;top:0;cursor:pointer}
        .pill span{display:block;min-width:64px;text-align:center;padding:11px 16px;border:2px solid var(--line);border-radius:14px;font-weight:800;background:#fff;transition:.18s}
        .pill:hover span{border-color:#fda4af;transform:translateY(-2px)}
        .pill input:checked+span{color:#fff;border-color:transparent;background:linear-gradient(135deg,#f43f5e,#be123c);box-shadow:0 10px 20px rgba(225,29,72,.35);transform:translateY(-2px) scale(1.05)}
        .pill input:focus-visible+span{box-shadow:0 0 0 4px rgba(251,113,133,.35)}

        .table-wrap{overflow-x:auto;border:1px solid var(--line);border-radius:16px}
        table{width:100%;border-collapse:collapse;min-width:640px}
        th{background:#fff1f3;color:var(--wine);text-align:left;padding:14px;font-size:.76rem;text-transform:uppercase;letter-spacing:.07em}
        td{padding:14px;border-top:1px solid var(--line)}
        tr:hover td{background:#fff8f9}
        .badge{padding:5px 12px;border-radius:999px;font-size:.8rem;font-weight:700;white-space:nowrap}
        .ok{background:#ccfbf1;color:#115e59}
        .no{background:#ffedd5;color:#9a3412}
        .bg{display:inline-block;background:#ffe4ea;color:var(--wine);padding:5px 11px;border-radius:10px;font-weight:800}
        .empty{text-align:center;padding:30px 10px;color:var(--muted)}
        .empty .big{font-size:2.8rem;margin-bottom:6px;animation:float 3s ease-in-out infinite}
        .mt{margin-top:22px}

        .toast{position:fixed;top:84px;right:20px;z-index:50;display:flex;align-items:center;gap:12px;max-width:340px;padding:14px 16px;
        border-radius:16px;font-weight:600;background:#fff;box-shadow:0 18px 40px rgba(30,27,46,.22);border-left:6px solid var(--teal);
        animation:slide .5s cubic-bezier(.34,1.56,.64,1);transition:opacity .5s,transform .5s}
        .toast.bad{border-left-color:var(--crimson)}
        .toast .ti{font-size:1.5rem;animation:pop .6s cubic-bezier(.34,1.56,.64,1) .1s backwards}
        .toast .x{margin-left:auto;border:0;background:none;font-size:1.3rem;cursor:pointer;color:var(--muted)}
        .toast.hide{opacity:0;transform:translateX(40px)}
        @keyframes slide{0%{transform:translateX(120%);opacity:0}100%{transform:none;opacity:1}}

        .page-hero{border-radius:28px;padding:46px 40px;color:#fff;text-align:center;position:relative;overflow:hidden;
        background:linear-gradient(135deg,#1e1b4b,#6d1a4f 55%,#e11d48);box-shadow:0 24px 54px rgba(109,26,79,.32)}
        .page-hero h1{margin:0 0 10px;font-size:clamp(1.8rem,4vw,2.8rem)}
        .page-hero p{margin:0 auto;max-width:620px;color:#ece9f7;line-height:1.65}
        .page-hero>*{position:relative;z-index:1}
        .feat{display:block}
        .tech{display:flex;flex-wrap:wrap;gap:10px;margin-top:6px}
        .tech span{background:#fff1f3;color:var(--wine);border:1px solid #fecdd3;padding:8px 16px;border-radius:999px;font-weight:700}
        details{border:1px solid var(--line);border-radius:14px;padding:14px 18px;margin-bottom:12px;background:#fff;transition:.2s}
        details[open]{border-color:#fda4af;box-shadow:0 8px 20px rgba(225,29,72,.1)}
        summary{cursor:pointer;font-weight:700}
        details p{margin:10px 0 0;color:var(--muted);line-height:1.6}
        .note{background:#fff7ed;border:1px solid #fed7aa;color:#9a3412;border-radius:16px;padding:16px 20px;line-height:1.55}
        footer{text-align:center;color:var(--muted);padding:30px 20px 40px;border-top:1px solid var(--line)}
        @media(prefers-reduced-motion:reduce){*{animation:none!important;transition:none!important}.js .reveal{opacity:1;transform:none}}
        """;

    static final String JS = """
        (function(){
          var els=document.querySelectorAll('.reveal');
          if('IntersectionObserver' in window){
            var io=new IntersectionObserver(function(entries){
              entries.forEach(function(e){if(e.isIntersecting){e.target.classList.add('in');io.unobserve(e.target);}});
            },{threshold:0.12});
            els.forEach(function(el){io.observe(el);});
          }else{els.forEach(function(el){el.classList.add('in');});}
          document.querySelectorAll('[data-count]').forEach(function(el){
            var target=parseInt(el.getAttribute('data-count'),10)||0;
            var start=null;
            function tick(t){
              if(!start){start=t;}
              var p=Math.min((t-start)/900,1);
              el.textContent=Math.round(target*(1-Math.pow(1-p,3)));
              if(p<1){requestAnimationFrame(tick);}
            }
            requestAnimationFrame(tick);
          });
          document.querySelectorAll('.toast').forEach(function(t){
            setTimeout(function(){t.classList.add('hide');setTimeout(function(){t.remove();},500);},4500);
          });
        })();
        """;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", WebServer::home);
        server.createContext("/add", WebServer::addDonor);
        server.createContext("/search", WebServer::search);
        server.createContext("/donate", WebServer::donate);
        server.createContext("/requests", WebServer::requests);
        server.createContext("/fulfill", WebServer::fulfill);
        server.createContext("/about", WebServer::about);
        server.start();
        System.out.println("Server started: http://localhost:8080/");
    }

    // ---------- Pages ----------

    static void home(HttpExchange ex) throws IOException {
        int donors = count("SELECT COUNT(*) FROM donors");
        int open = count("SELECT COUNT(*) FROM requests WHERE status = 'Open'");
        int done = count("SELECT COUNT(*) FROM requests WHERE status = 'Fulfilled'");
        String body;
        if (donors < 0) {
            body = toast("bad", "Database not connected. Check the terminal.");
        } else {
            body = "<section class='hero'>"
                + "<div class='blob b1'></div><div class='blob b2'></div>"
                + "<div><span class='tag'>&#10084;&#65039; Community blood network</span>"
                + "<h1>Every drop counts.<br><span class='grad'>Be someone's hero.</span></h1>"
                + "<p>Find donors by blood group and city, or raise an urgent request in seconds. "
                + "Simple, fast and built to save lives.</p>"
                + "<div class='actions'>"
                + "<a class='btn' href='/add'>&#128137; Become a donor</a>"
                + "<a class='btn ghost' href='/search'>&#128269; Find a donor</a></div>"
                + "<svg class='ecg' viewBox='0 0 260 60' aria-hidden='true'>"
                + "<path d='M0 32 H50 L60 32 L68 10 L80 54 L90 22 L98 32 H160 L170 32 L178 10 L190 54 L200 22 L208 32 H260'/></svg>"
                + "</div>"
                + "<div class='drop-wrap'><div class='ring'></div><div class='ring r2'></div>"
                + "<div class='drop'>&#129656;</div>"
                + "<span class='fchip' style='top:14%;left:6%;--d:0s'>O+</span>"
                + "<span class='fchip' style='top:20%;right:4%;--d:.8s'>A-</span>"
                + "<span class='fchip' style='bottom:18%;left:2%;--d:1.6s'>B+</span>"
                + "<span class='fchip' style='bottom:12%;right:8%;--d:2.4s'>AB+</span></div>"
                + "</section>"
                + "<div class='stats'>"
                + stat("&#128101;", donors, "Registered donors", "0s")
                + stat("&#128680;", open, "Open requests", ".12s")
                + stat("&#128154;", done, "Requests fulfilled", ".24s")
                + "</div>"
                + "<h2 class='section-title reveal'><small>Simple &amp; quick</small>How it works</h2><div class='grid'>"
                + step("1", "&#128221;", "Register", "Add your details and blood group in under a minute.", "0s")
                + step("2", "&#128269;", "Get matched", "Anyone in need can search donors by blood group and city.", ".12s")
                + step("3", "&#10084;&#65039;", "Save a life", "Donate, mark it done, and stay safe with the 90-day rule.", ".24s")
                + "</div>"
                + "<div class='cta reveal'><h2>Ready to make a difference?</h2>"
                + "<p class='sub'>One donation can help save up to three lives.</p>"
                + "<a class='btn outline' href='/add'>Join as a donor</a>"
                + "<a class='btn ghost' href='/requests'>Need blood?</a></div>";
        }
        send(ex, page("/", body));
    }

    static void about(HttpExchange ex) throws IOException {
        String body = "<section class='page-hero'>"
            + "<div class='blob b1'></div><div class='blob b2'></div>"
            + "<span class='tag'>&#129309; About us</span>"
            + "<h1>Connecting donors with people in need</h1>"
            + "<p>Blood Donor Network is a simple web app that helps people find willing blood donors "
            + "in their city, quickly and without the usual phone-call chaos.</p></section>"

            + "<div class='card lift reveal mt'><div class='head'><div class='chip'>&#127919;</div>"
            + "<div><h2>Our mission</h2><p class='sub'>Make finding blood as easy as a search.</p></div></div>"
            + "<p class='sub'>When someone needs blood urgently, time matters. This project brings donors "
            + "and patients together in one place: donors register once, and anyone can search by blood group "
            + "and city. Requests can be raised, tracked and marked fulfilled, so nobody is left waiting.</p></div>"

            + "<h2 class='section-title reveal'><small>What you can do</small>Features</h2><div class='grid'>"
            + feat("&#128221;", "Register as a donor", "Add your name, age, blood group, city and phone number.", "0s")
            + feat("&#128269;", "Smart search", "Pick a blood group, type a city and see matching donors instantly.", ".1s")
            + feat("&#128680;", "Urgent requests", "Raise a request for a patient and track it until it is fulfilled.", ".2s")
            + feat("&#128737;&#65039;", "90-day safety rule", "Donors are marked not eligible until 90 days have passed since their last donation.", ".3s")
            + "</div>"

            + "<h2 class='section-title reveal'><small>Good to know</small>Who can donate to whom</h2>"
            + "<div class='card reveal'><p class='sub'>Compatibility for red blood cell donations.</p>"
            + "<div class='table-wrap'><table><tr><th>Blood group</th><th>Can donate to</th><th>Can receive from</th></tr>"
            + compatRows() + "</table></div></div>"

            + "<h2 class='section-title reveal'><small>Questions</small>FAQ</h2><div class='reveal'>"
            + "<details><summary>Who can register as a donor?</summary><p>Healthy adults aged 18 to 65 can register here. "
            + "The final decision on eligibility is always made by the medical staff at the donation centre.</p></details>"
            + "<details><summary>Why is there a 90-day gap?</summary><p>The body needs time to replace the blood it gave. "
            + "This app uses a 90-day gap as a simple safety rule and hides the Mark donated button until it has passed. "
            + "Real-world gaps can differ by country and donation type.</p></details>"
            + "<details><summary>How do I find a donor?</summary><p>Open Find Donors, choose the blood group, type the city "
            + "and press Search. You will see names, phone numbers and whether each donor is currently eligible.</p></details>"
            + "<details><summary>Where is my data stored?</summary><p>In a MySQL database on the computer running this app. "
            + "This is a student project, so do not enter anything you would not be comfortable seeing on screen.</p></details></div>"

            + "<div class='card reveal mt'><div class='head'><div class='chip'>&#128187;</div>"
            + "<div><h2>About the project</h2><p class='sub'>Built as a Java mini project.</p></div></div>"
            + "<div class='tech'><span>Java</span><span>JDBC</span><span>MySQL</span><span>Java HttpServer</span><span>HTML &amp; CSS</span></div></div>"

            + "<div class='note reveal mt'>&#128161; This is an educational project and not medical advice. "
            + "In an emergency, contact a hospital or blood bank directly.</div>";
        send(ex, page("/about", body));
    }

    static void addDonor(HttpExchange ex) throws IOException {
        String message = "";
        if (ex.getRequestMethod().equals("POST")) {
            Map<String, String> f = parseForm(readBody(ex));
            try {
                int age = Integer.parseInt(f.getOrDefault("age", "").trim());
                boolean ok = DonorDAO.addDonor(
                    f.getOrDefault("name", "").trim(), age,
                    f.getOrDefault("blood_group", ""),
                    f.getOrDefault("city", "").trim(),
                    f.getOrDefault("phone", "").trim());
                message = ok ? toast("good", "Donor added successfully!")
                             : toast("bad", "Could not save. Check the terminal.");
            } catch (NumberFormatException e) {
                message = toast("bad", "Please enter a valid age.");
            }
        }
        String body = message + "<div class='card reveal'>"
            + head("&#128137;", "Become a donor", "Fill in your details so people in need can reach you.")
            + "<form method='post' action='/add'>"
            + "<div class='row'>"
            + "<div><label class='l'>Full name</label><input class='in' name='name' placeholder='e.g. Ravi Kumar' required></div>"
            + "<div><label class='l'>Age</label><input class='in' name='age' type='number' min='18' max='65' placeholder='18 - 65' required></div>"
            + "</div>"
            + "<label class='l'>Blood group</label><div class='pills'>" + bloodPills("") + "</div>"
            + "<div class='row'>"
            + "<div><label class='l'>City</label><input class='in' name='city' placeholder='e.g. Chennai' required></div>"
            + "<div><label class='l'>Phone</label><input class='in' name='phone' pattern='[0-9]{10}' title='Enter a 10-digit phone number' placeholder='10-digit number' required></div>"
            + "</div><br><button class='btn' type='submit'>Add donor</button></form></div>";
        send(ex, page("/add", body));
    }

    static void search(HttpExchange ex) throws IOException {
        String query = ex.getRequestURI().getRawQuery();
        Map<String, String> q = parseForm(query == null ? "" : query);
        String bg = q.getOrDefault("blood_group", "");
        String city = q.getOrDefault("city", "").trim();

        String body = "<div class='card reveal'>"
            + head("&#128269;", "Find a donor", "Pick a blood group and enter a city.")
            + "<form method='get' action='/search'>"
            + "<label class='l'>Blood group</label><div class='pills'>" + bloodPills(bg) + "</div>"
            + "<label class='l'>City</label>"
            + "<input class='in' name='city' value='" + esc(city) + "' placeholder='e.g. Chennai' required>"
            + "<br><br><button class='btn' type='submit'>Search donors</button></form></div>";

        if (!bg.isEmpty() && !city.isEmpty()) {
            List<String[]> rows = DonorDAO.search(bg, city);
            body += "<div class='card reveal mt'>";
            if (rows.isEmpty()) {
                body += "<div class='empty'><div class='big'>&#128533;</div>"
                      + "No donors found for <b>" + esc(bg) + "</b> in <b>" + esc(city) + "</b>.</div>";
            } else {
                body += "<h2>" + rows.size() + " donor(s) found</h2><p class='sub'>Showing "
                      + esc(bg) + " donors in " + esc(city) + "</p>"
                      + "<div class='table-wrap'><table>"
                      + "<tr><th>ID</th><th>Name</th><th>Blood</th><th>City</th><th>Phone</th>"
                      + "<th>Last donated</th><th>Status</th><th>Action</th></tr>";
                for (String[] r : rows) {
                    boolean eligible = r[6].equals("Eligible");
                    body += "<tr><td>" + esc(r[0]) + "</td><td><b>" + esc(r[1]) + "</b></td>"
                          + "<td><span class='bg'>" + esc(r[2]) + "</span></td>"
                          + "<td>" + esc(r[3]) + "</td><td>" + esc(r[4]) + "</td><td>" + esc(r[5]) + "</td>"
                          + "<td><span class='badge " + (eligible ? "ok" : "no") + "'>" + esc(r[6]) + "</span></td><td>";
                    if (eligible) {
                        body += "<form method='post' action='/donate'>"
                              + hidden("id", r[0]) + hidden("blood_group", bg) + hidden("city", city)
                              + "<button class='btn small' type='submit'>Mark donated</button></form>";
                    } else {
                        body += "-";
                    }
                    body += "</td></tr>";
                }
                body += "</table></div>";
            }
            body += "</div>";
        }
        send(ex, page("/search", body));
    }

    static void donate(HttpExchange ex) throws IOException {
        Map<String, String> f = parseForm(readBody(ex));
        try {
            DonorDAO.markDonated(Integer.parseInt(f.getOrDefault("id", "").trim()));
        } catch (NumberFormatException e) {
            System.out.println("Bad donor id");
        }
        redirect(ex, "/search?blood_group="
            + URLEncoder.encode(f.getOrDefault("blood_group", ""), StandardCharsets.UTF_8)
            + "&city=" + URLEncoder.encode(f.getOrDefault("city", ""), StandardCharsets.UTF_8));
    }

    static void requests(HttpExchange ex) throws IOException {
        String message = "";
        if (ex.getRequestMethod().equals("POST")) {
            Map<String, String> f = parseForm(readBody(ex));
            boolean ok = RequestDAO.addRequest(
                f.getOrDefault("patient_name", "").trim(),
                f.getOrDefault("blood_group", ""),
                f.getOrDefault("city", "").trim(),
                f.getOrDefault("contact", "").trim());
            message = ok ? toast("good", "Request raised successfully!")
                         : toast("bad", "Could not save. Check the terminal.");
        }

        String body = message + "<div class='card reveal'>"
            + head("&#128680;", "Raise a blood request", "Tell us who needs blood and where.")
            + "<form method='post' action='/requests'>"
            + "<div class='row'>"
            + "<div><label class='l'>Patient name</label><input class='in' name='patient_name' required></div>"
            + "<div><label class='l'>Contact number</label><input class='in' name='contact' pattern='[0-9]{10}' title='Enter a 10-digit phone number' placeholder='10-digit number' required></div>"
            + "</div>"
            + "<label class='l'>Blood group needed</label><div class='pills'>" + bloodPills("") + "</div>"
            + "<label class='l'>City</label><input class='in' name='city' placeholder='e.g. Chennai' required>"
            + "<br><br><button class='btn' type='submit'>Raise request</button></form></div>";

        List<String[]> open = RequestDAO.listOpen();
        body += "<div class='card reveal mt'>"
            + head("&#128276;", "Open requests", "People waiting for help right now.");
        if (open.isEmpty()) {
            body += "<div class='empty'><div class='big'>&#127881;</div>No open requests right now.</div>";
        } else {
            body += "<div class='table-wrap'><table>"
                + "<tr><th>ID</th><th>Patient</th><th>Blood</th><th>City</th><th>Contact</th><th>Action</th></tr>";
            for (String[] r : open) {
                String find = "/search?blood_group=" + URLEncoder.encode(r[2], StandardCharsets.UTF_8)
                            + "&city=" + URLEncoder.encode(r[3], StandardCharsets.UTF_8);
                body += "<tr><td>" + esc(r[0]) + "</td><td><b>" + esc(r[1]) + "</b></td>"
                      + "<td><span class='bg'>" + esc(r[2]) + "</span></td>"
                      + "<td>" + esc(r[3]) + "</td><td>" + esc(r[4]) + "</td><td>"
                      + "<a class='btn small outline' href='" + esc(find) + "'>Find donors</a> "
                      + "<form style='display:inline' method='post' action='/fulfill'>"
                      + hidden("id", r[0])
                      + "<button class='btn small' type='submit'>Mark fulfilled</button></form>"
                      + "</td></tr>";
            }
            body += "</table></div>";
        }
        body += "</div>";
        send(ex, page("/requests", body));
    }

    static void fulfill(HttpExchange ex) throws IOException {
        Map<String, String> f = parseForm(readBody(ex));
        try {
            RequestDAO.markFulfilled(Integer.parseInt(f.getOrDefault("id", "").trim()));
        } catch (NumberFormatException e) {
            System.out.println("Bad request id");
        }
        redirect(ex, "/requests");
    }

    // ---------- Layout helpers ----------

    static String page(String active, String body) {
        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
            + "<title>Blood Donor Network</title>"
            + "<link href='https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700;800&display=swap' rel='stylesheet'>"
            + "<script>document.documentElement.className='js'</script>"
            + "<style>" + CSS + "</style></head><body>"
            + "<nav><a class='brand' href='/'><span class='logo'>&#129656;</span>Blood Donor Network</a>"
            + "<div class='links'>"
            + navLink("/", "Home", active) + navLink("/add", "Add Donor", active)
            + navLink("/search", "Find Donors", active) + navLink("/requests", "Blood Requests", active)
            + navLink("/about", "About", active)
            + "</div></nav><main>" + body + "</main>"
            + "<footer>Made with &#10084;&#65039; to save lives. One donation can help save up to three.</footer>"
            + "<script>" + JS + "</script></body></html>";
    }

    static String navLink(String href, String label, String active) {
        return "<a href='" + href + "'" + (href.equals(active) ? " class='active'" : "") + ">" + label + "</a>";
    }

    static String head(String icon, String title, String sub) {
        return "<div class='head'><div class='chip'>" + icon + "</div><div><h2>" + title
            + "</h2><p class='sub'>" + sub + "</p></div></div>";
    }

    static String stat(String icon, int n, String label, String delay) {
        return "<div class='card lift stat reveal' style='--d:" + delay + "'><div class='chip' style='--d:" + delay
            + "'>" + icon + "</div><div><div class='num' data-count='" + n + "'>" + n
            + "</div><div class='label'>" + label + "</div></div></div>";
    }

    static String step(String n, String icon, String title, String text, String delay) {
        return "<div class='card lift step reveal' style='--d:" + delay + "'><span class='n'>" + n
            + "</span><div class='chip' style='--d:" + delay + "'>" + icon + "</div><h3>" + title
            + "</h3><p>" + text + "</p></div>";
    }

    static String feat(String icon, String title, String text, String delay) {
        return "<div class='card lift feat reveal' style='--d:" + delay + "'><div class='chip' style='--d:" + delay
            + "'>" + icon + "</div><h3>" + title + "</h3><p>" + text + "</p></div>";
    }

    static String compatRows() {
        String rows = "";
        for (String[] r : COMPAT) {
            rows += "<tr><td><span class='bg'>" + esc(r[0]) + "</span></td><td>" + esc(r[1])
                  + "</td><td>" + esc(r[2]) + "</td></tr>";
        }
        return rows;
    }

    static String toast(String type, String text) {
        String icon = type.equals("good") ? "&#9989;" : "&#9888;&#65039;";
        return "<div class='toast " + type + "' role='status'><span class='ti'>" + icon + "</span><span>"
            + text + "</span><button class='x' type='button' onclick='this.parentNode.remove()'>&times;</button></div>";
    }

    static String hidden(String name, String value) {
        return "<input type='hidden' name='" + name + "' value='" + esc(value) + "'>";
    }

    static String bloodPills(String selected) {
        String out = "";
        for (String g : BLOOD_GROUPS) {
            out += "<label class='pill'><input type='radio' name='blood_group' value='" + esc(g) + "'"
                 + (g.equals(selected) ? " checked" : "") + " required><span>" + esc(g) + "</span></label>";
        }
        return out;
    }

    // ---------- Utilities ----------

    static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    static String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    static Map<String, String> parseForm(String body) {
        Map<String, String> map = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    static int count(String sql) {
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        } catch (Exception e) {
            System.out.println("DB error: " + e.getMessage());
            return -1;
        }
    }

    static void redirect(HttpExchange ex, String to) throws IOException {
        ex.getResponseHeaders().set("Location", to);
        ex.sendResponseHeaders(303, -1);
        ex.close();
    }

    static void send(HttpExchange ex, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}