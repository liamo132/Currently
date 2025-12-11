// components/header-user.jsx
import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import './header-user.css';

const Header = ({ activePage }) => {
  const navigate = useNavigate();

  const pages = [
    { name: 'Dashboard',         id: 'dashboard',      path: '/dashboard' },
    { name: 'Map My House',      id: 'mapmyhouse',     path: '/mapmyhouse' },
    { name: 'My Appliances',     id: 'myappliances',   path: '/my-appliances' },
    { name: 'Watch your Watts',  id: 'watchyourwatts', path: '/watchyourwatts' }
  ];

  const handleLogout = () => {
    localStorage.removeItem('token');
    navigate('/login', { replace: true });
  };

  return (
    <header className="user-header">
      <nav className="user-nav">
        <h1 className="logo">Currently.</h1>

        <ul>
          {pages.map((page) => (
            <li key={page.id}>
              <Link
                to={page.path}
                className={activePage === page.id ? 'active' : ''}
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
