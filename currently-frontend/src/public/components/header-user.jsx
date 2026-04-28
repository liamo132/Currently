// components/header-user.jsx
import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Menu, X } from 'lucide-react';
import './header-user.css';

const Header = ({ activePage }) => {
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  const pages = [
    { name: 'Dashboard',         id: 'dashboard',      path: '/dashboard' },
    { name: 'Map My House',      id: 'mapmyhouse',     path: '/mapmyhouse' },
    { name: 'My Appliances',     id: 'myappliances',   path: '/my-appliances' },
    { name: 'Watch your Watts',  id: 'watchyourwatts', path: '/watchyourwatts' },
    { name: 'Smart Insights',    id: 'smartinsights',  path: '/smartinsights' }
  ];

  const handleLogout = () => {
    localStorage.removeItem('token');
    setMenuOpen(false);
    navigate('/login', { replace: true });
  };

  useEffect(() => {
    if (!menuOpen) return;
    const handleResize = () => {
      if (window.innerWidth > 768) setMenuOpen(false);
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [menuOpen]);

  return (
    <header className="user-header">
      <nav className="user-nav">
        <Link to="/dashboard" className="logo user-logo" onClick={() => setMenuOpen(false)}>
          Currently.
        </Link>

        <button
          type="button"
          className="user-menu-toggle"
          onClick={() => setMenuOpen((open) => !open)}
          aria-expanded={menuOpen}
          aria-controls="user-navigation"
          aria-label={menuOpen ? 'Close navigation' : 'Open navigation'}
        >
          {menuOpen ? <X size={22} /> : <Menu size={22} />}
        </button>

        <ul id="user-navigation" className={menuOpen ? 'is-open' : ''}>
          {pages.map((page) => (
            <li key={page.id}>
              <Link
                to={page.path}
                className={activePage === page.id ? 'active' : ''}
                onClick={() => setMenuOpen(false)}
              >
                {page.name}
              </Link>
            </li>
          ))}

          <li>
            <button
              type="button"
              className="logout-link"
              onClick={handleLogout}
            >
              Log out
            </button>
          </li>
        </ul>
      </nav>
    </header>
  );
};

export default Header;
